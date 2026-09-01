package com.xlxyvergil.fusedcritical.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.text.DecimalFormat;

// 客户端伤害数字粒子：在受击方头顶渲染一段文本数字，随暴击等级着色（普通白 / 1 级橙 / 2 级及以上红）。
// 写法参考 DuMmmMmmy 的自定义粒子：用 Font.drawInBatch 直接绘制文本，而不是贴图精灵，
// 因此不依赖粒子贴图纹理，也不走精灵渲染流程。
@OnlyIn(Dist.CLIENT)
public final class DamageNumberParticle extends Particle {

    // 只显示一位小数，避免超长小数刷屏
    private static final DecimalFormat AMOUNT = new DecimalFormat("#.#");

    private final Font font = Minecraft.getInstance().font;
    private final Component text;
    private final int color;
    private final int darkColor;

    private float fadeout = -1;
    private float prevFadeout = -1;
    private float visualDY = 0;
    private float prevVisualDY = 0;
    private float visualDX = 0;
    private float prevVisualDX = 0;

    // 按暴击等级取颜色：0 -> 白，1 -> 橙，>=2 -> 红
    private static int colorForTier(int tier) {
        return tier <= 0 ? 0xFFFFFF : (tier == 1 ? 0xFF8C1A : 0xFF2020);
    }

    public DamageNumberParticle(ClientLevel level, double x, double y, double z,
                                double amount, double tier) {
        super(level, x, y, z);
        this.lifetime = 35;

        this.color = colorForTier((int) tier);
        int r = (this.color >> 16) & 0xFF;
        int g = (this.color >> 8) & 0xFF;
        int b = this.color & 0xFF;
        this.setColor(r / 255.0F, g / 255.0F, b / 255.0F);
        // 阴影用主色压暗，保证在不同亮度下都能看清数字
        this.darkColor = FastColor.ARGB32.color(255, (int) (r * 0.35F), (int) (g * 0.35F), (int) (b * 0.35F));

        this.text = Component.literal(AMOUNT.format(Math.abs(amount)));

        // 向上漂 + 轻微横向漂移，让多次命中时数字错开
        this.yd = 1.0D;
        this.xd = (level.random.nextDouble() - 0.5D) * 0.2D;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        // 结尾若干 tick 做淡出，其余时间保持不透明
        float fadeDuration = 6.0F;
        this.prevFadeout = this.fadeout;
        this.fadeout = this.age > (this.lifetime - fadeDuration)
                ? ((float) this.lifetime - this.age) / fadeDuration
                : 1.0F;

        this.prevVisualDY = this.visualDY;
        this.visualDY += (float) this.yd;
        this.prevVisualDX = this.visualDX;
        this.visualDX += (float) this.xd;

        // 参考 DuMmmMmmy：让数字围绕受击点上方做椭圆运动。
        // 尚未飞出椭圆时逐步减速（yd 减半）；飞出椭圆后彻底停住（yd/xd 归零）。
        // 避免数字一直以 yd=1 向上飞，导致飞得又高又快、脱离受击者。
        if (Math.sqrt(Mth.square(this.visualDX * 1.5) + Mth.square(this.visualDY - 1)) < 0.9) {
            this.yd = this.yd / 2.0D;
        } else {
            this.yd = 0.0D;
            this.xd = 0.0D;
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks) {
        Vec3 camPos = camera.getPosition();
        float px = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camPos.x());
        float py = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camPos.y());
        float pz = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camPos.z());

        PoseStack pose = new PoseStack();
        pose.pushPose();
        pose.translate(px, py, pz);

        double distFromCam = new Vec3(px, py, pz).length();
        // 按距离缩放，使不同距离下字号基本保持恒定
        float scale = (float) (0.006F * distFromCam);
        float fade = Mth.lerp(partialTicks, this.prevFadeout, this.fadeout);

        pose.mulPose(camera.rotation());
        pose.translate(Mth.lerp(partialTicks, this.prevVisualDX, this.visualDX),
                Mth.lerp(partialTicks, this.prevVisualDY, this.visualDY), 0);
        pose.scale(-scale, -scale, scale);
        pose.translate(0, (4.0D * (1.0D - fade)), 0);
        pose.scale(fade, fade, fade);
        pose.translate(0, -distFromCam / 10.0D, 0);

        var buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);

        float x1 = 0.5F - this.font.width(this.text) / 2.0F;
        this.font.drawInBatch(this.text, x1, 0, this.color, false,
                pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        pose.translate(1, 1, 0.03D);
        this.font.drawInBatch(this.text, x1, 0, this.darkColor, false,
                pose.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        buffer.endBatch();
        pose.popPose();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.CUSTOM;
    }
}
