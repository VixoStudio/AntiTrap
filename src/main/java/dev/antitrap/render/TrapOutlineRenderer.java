package dev.antitrap.render;

import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

import dev.antitrap.AntiTrapMod;
import dev.antitrap.config.AntiTrapConfig;
import dev.antitrap.detect.DangerLevel;
import dev.antitrap.detect.TrapCandidate;

/**
 * Thin camera-relative wireframe. Nearby / high-threat blocks get a full cage;
 * far low-threat blocks only get corner ticks so a long-range scan does not flood the GPU.
 */
public final class TrapOutlineRenderer {
	private static final RenderPipeline OUTLINE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(AntiTrapMod.MOD_ID, "pipeline/trap_outline"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.withCull(false)
			.build()
	);

	private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(1 << 22);
	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
	private static final float PAD = 0.018f;
	private static final float NEAR_DIST_SQ = 16f * 16f;

	private BufferBuilder buffer;
	private MappableRingBuffer vertexBuffer;

	public void register() {
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(this::extractAndDraw);
	}

	public void invalidate() {
		buffer = null;
	}

	public void shutdown() {
		buffer = null;
		if (vertexBuffer != null) {
			vertexBuffer.close();
			vertexBuffer = null;
		}
	}

	private void extractAndDraw(WorldRenderContext context) {
		if (!AntiTrapMod.isActive()) {
			return;
		}
		List<TrapCandidate> hits = AntiTrapMod.scanner().result().highlights;
		if (hits.isEmpty()) {
			return;
		}
		extract(context, hits);
		draw(Minecraft.getInstance());
	}

	private void extract(WorldRenderContext context, List<TrapCandidate> hits) {
		PoseStack matrices = context.matrices();
		if (matrices == null) {
			matrices = new PoseStack();
		}
		Vec3 camera = context.worldState().cameraRenderState.pos;
		matrices.pushPose();
		matrices.translate(-camera.x, -camera.y, -camera.z);
		Matrix4fc pose = matrices.last().pose();

		if (buffer == null) {
			buffer = new BufferBuilder(ALLOCATOR, OUTLINE.getVertexFormatMode(), OUTLINE.getVertexFormat());
		}

		AntiTrapConfig config = AntiTrapMod.config();
		float intensity = config.outlineAlpha();
		float camX = (float) camera.x;
		float camY = (float) camera.y;
		float camZ = (float) camera.z;
		for (int i = 0, n = hits.size(); i < n; i++) {
			TrapCandidate hit = hits.get(i);
			int argb = config.colorFor(hit.level);
			float r = ((argb >>> 16) & 0xFF) / 255f;
			float g = ((argb >>> 8) & 0xFF) / 255f;
			float b = (argb & 0xFF) / 255f;
			r = r * 0.82f + 0.18f;
			g = g * 0.82f + 0.18f;
			b = b * 0.82f + 0.18f;
			float dx = hit.x + 0.5f - camX;
			float dy = hit.y + 0.5f - camY;
			float dz = hit.z + 0.5f - camZ;
			float distSq = dx * dx + dy * dy + dz * dz;
			boolean high = hit.level == DangerLevel.HIGH;
			boolean near = distSq <= NEAR_DIST_SQ;
			float a = intensity * (high ? 0.92f : near ? 0.78f : 0.58f);
			float x0 = hit.x - PAD;
			float y0 = hit.y - PAD;
			float z0 = hit.z - PAD;
			float x1 = hit.x + 1f + PAD;
			float y1 = hit.y + 1f + PAD;
			float z1 = hit.z + 1f + PAD;
			float thick = high ? 0.038f : 0.028f;
			if (high || near) {
				if (high && near) {
					addFill(pose, buffer, x0, y0, z0, x1, y1, z1, r, g, b, a * 0.16f);
				}
				addWire(pose, buffer, x0, y0, z0, x1, y1, z1, r, g, b, a, thick);
			} else {
				addCorners(pose, buffer, x0, y0, z0, x1, y1, z1, r, g, b, a, thick, 0.26f);
			}
		}
		matrices.popPose();
	}

	private void draw(Minecraft client) {
		MeshData mesh = buffer.buildOrThrow();
		MeshData.DrawState draw = mesh.drawState();
		VertexFormat format = draw.format();
		GpuBuffer vertices = upload(draw, format, mesh);
		submit(client, draw, vertices);
		if (vertexBuffer != null) {
			vertexBuffer.rotate();
		}
		buffer = null;
		mesh.close();
	}

	private GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData mesh) {
		int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();
		if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
			if (vertexBuffer != null) {
				vertexBuffer.close();
			}
			vertexBuffer = new MappableRingBuffer(() -> "antitrap outline", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
		}
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		try (GpuBuffer.MappedView mapped = encoder.mapBuffer(vertexBuffer.currentBuffer().slice(0, mesh.vertexBuffer().remaining()), false, true)) {
			MemoryUtil.memCopy(mesh.vertexBuffer(), mapped.data());
		}
		return vertexBuffer.currentBuffer();
	}

	private static void submit(Minecraft client, MeshData.DrawState draw, GpuBuffer vertices) {
		RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(OUTLINE.getVertexFormatMode());
		GpuBuffer indices = shapeIndexBuffer.getBuffer(draw.indexCount());
		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
				.writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
		try (RenderPass pass = RenderSystem.getDevice()
				.createCommandEncoder()
				.createRenderPass(() -> "antitrap outline", client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(), client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
			pass.setPipeline(OUTLINE);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transforms);
			pass.setVertexBuffer(0, vertices);
			pass.setIndexBuffer(indices, shapeIndexBuffer.type());
			pass.drawIndexed(0, 0, draw.indexCount(), 1);
		}
	}

	private static void addFill(Matrix4fc pose, BufferBuilder buf,
			float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
			float r, float g, float b, float a) {
		quad(pose, buf, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
		quad(pose, buf, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
		quad(pose, buf, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
		quad(pose, buf, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
		quad(pose, buf, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
		quad(pose, buf, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
	}

	private static void addWire(Matrix4fc pose, BufferBuilder buf,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float r, float g, float b, float a, float t) {
		xEdge(pose, buf, x0, x1, y0, z0, t, r, g, b, a);
		xEdge(pose, buf, x0, x1, y0, z1, t, r, g, b, a);
		xEdge(pose, buf, x0, x1, y1, z0, t, r, g, b, a);
		xEdge(pose, buf, x0, x1, y1, z1, t, r, g, b, a);
		yEdge(pose, buf, y0, y1, x0, z0, t, r, g, b, a);
		yEdge(pose, buf, y0, y1, x0, z1, t, r, g, b, a);
		yEdge(pose, buf, y0, y1, x1, z0, t, r, g, b, a);
		yEdge(pose, buf, y0, y1, x1, z1, t, r, g, b, a);
		zEdge(pose, buf, z0, z1, x0, y0, t, r, g, b, a);
		zEdge(pose, buf, z0, z1, x0, y1, t, r, g, b, a);
		zEdge(pose, buf, z0, z1, x1, y0, t, r, g, b, a);
		zEdge(pose, buf, z0, z1, x1, y1, t, r, g, b, a);
	}

	private static void addCorners(Matrix4fc pose, BufferBuilder buf,
			float x0, float y0, float z0, float x1, float y1, float z1,
			float r, float g, float b, float a, float t, float len) {
		corner(pose, buf, x0, y0, z0, len, len, len, t, r, g, b, a);
		corner(pose, buf, x1, y0, z0, -len, len, len, t, r, g, b, a);
		corner(pose, buf, x0, y1, z0, len, -len, len, t, r, g, b, a);
		corner(pose, buf, x1, y1, z0, -len, -len, len, t, r, g, b, a);
		corner(pose, buf, x0, y0, z1, len, len, -len, t, r, g, b, a);
		corner(pose, buf, x1, y0, z1, -len, len, -len, t, r, g, b, a);
		corner(pose, buf, x0, y1, z1, len, -len, -len, t, r, g, b, a);
		corner(pose, buf, x1, y1, z1, -len, -len, -len, t, r, g, b, a);
	}

	private static void corner(Matrix4fc pose, BufferBuilder buf,
			float x, float y, float z, float dx, float dy, float dz, float t,
			float r, float g, float b, float a) {
		xEdge(pose, buf, x, x + dx, y, z, t, r, g, b, a);
		yEdge(pose, buf, y, y + dy, x, z, t, r, g, b, a);
		zEdge(pose, buf, z, z + dz, x, y, t, r, g, b, a);
	}

	private static void xEdge(Matrix4fc pose, BufferBuilder buf,
			float x0, float x1, float y, float z, float t, float r, float g, float b, float a) {
		quad(pose, buf, x0, y - t, z, x1, y - t, z, x1, y + t, z, x0, y + t, z, r, g, b, a);
		quad(pose, buf, x0, y, z - t, x1, y, z - t, x1, y, z + t, x0, y, z + t, r, g, b, a);
	}

	private static void yEdge(Matrix4fc pose, BufferBuilder buf,
			float y0, float y1, float x, float z, float t, float r, float g, float b, float a) {
		quad(pose, buf, x - t, y0, z, x - t, y1, z, x + t, y1, z, x + t, y0, z, r, g, b, a);
		quad(pose, buf, x, y0, z - t, x, y1, z - t, x, y1, z + t, x, y0, z + t, r, g, b, a);
	}

	private static void zEdge(Matrix4fc pose, BufferBuilder buf,
			float z0, float z1, float x, float y, float t, float r, float g, float b, float a) {
		quad(pose, buf, x - t, y, z0, x - t, y, z1, x + t, y, z1, x + t, y, z0, r, g, b, a);
		quad(pose, buf, x, y - t, z0, x, y - t, z1, x, y + t, z1, x, y + t, z0, r, g, b, a);
	}

	private static void quad(Matrix4fc pose, BufferBuilder buf,
			float x1, float y1, float z1,
			float x2, float y2, float z2,
			float x3, float y3, float z3,
			float x4, float y4, float z4,
			float r, float g, float b, float a) {
		buf.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
		buf.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
		buf.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
		buf.addVertex(pose, x4, y4, z4).setColor(r, g, b, a);
	}

	public void close() {
		ALLOCATOR.close();
		if (vertexBuffer != null) {
			vertexBuffer.close();
			vertexBuffer = null;
		}
	}
}
