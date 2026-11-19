package com.atomiclegacy.inventory.gui;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.atomiclegacy.AtomicLegacy;
import com.atomiclegacy.inventory.container.WeaponTableMenu;
import com.atomiclegacy.items.weapon.sedna.ItemGunBaseNT;
import com.atomiclegacy.render.ItemRenderWeaponBase;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.lwjgl.glfw.GLFW;

/** 1.7.10 GUIWeaponTable: 176x240, draggable 3D preview of the gun, config index toggle. */
public class WeaponTableScreen extends AbstractContainerScreen<WeaponTableMenu> {

	public static final ResourceLocation texture = AtomicLegacy.id("textures/gui/machine/gui_weapon_modifier.png");

	public double yaw = 20;
	public double pitch = -10;

	public WeaponTableScreen(WeaponTableMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title);
		this.imageWidth = 176;
		this.imageHeight = 240;
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, float interp) {
		this.renderBackground(graphics, x, y, interp);
		super.render(graphics, x, y, interp);
		this.renderTooltip(graphics, x, y);

		if(leftPos + 8 <= x && leftPos + 8 + 160 > x && topPos + 18 < y && topPos + 18 + 79 >= y) {
			if(GLFW.glfwGetMouseButton(this.minecraft.getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
				double distX = (leftPos + 8 + 80) - x;
				double distY = (topPos + 18 + 39.5) - y;
				yaw = distX / 80D * -180D;
				pitch = distY / 39.5D * 90D;
			}
		}
	}

	@Override
	public boolean mouseClicked(double x, double y, int key) {
		boolean handled = super.mouseClicked(x, y, key);

		if(leftPos + 26 <= x && leftPos + 26 + 7 > x && topPos + 111 < y && topPos + 111 + 10 >= y) {
			ItemStack gun = this.menu.gun.getItem(0);
			if(!gun.isEmpty() && gun.getItem() instanceof ItemGunBaseNT gunItem) {
				int configs = gunItem.getConfigCount();
				if(configs > 1) {
					this.menu.index++;
					this.menu.index %= configs;
					this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.menu.index);
				}
			}
		}
		return handled;
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mX, int mY) {
		graphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0xffffff, false);
		graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float inter, int mX, int mY) {
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

		ItemStack gun = this.menu.gun.getItem(0);

		if(!gun.isEmpty() && gun.getItem() instanceof ItemGunBaseNT) {
			graphics.blit(texture, leftPos + 35, topPos + 112, 176 + 6 * this.menu.index, 0, 6, 8);

			if(IClientItemExtensions.of(gun).getCustomRenderer() instanceof ItemRenderWeaponBase renderGun) {
				PoseStack pose = graphics.pose();
				pose.pushPose();
				pose.translate(leftPos + 88, topPos + 57, 100);

				// RenderHelper.enableStandardItemLighting() under glRotated(180, 1, 0, 0)
				Matrix4f lightRotation = new Matrix4f().rotateX((float) Math.PI);
				RenderSystem.setShaderLights(
						lightRotation.transformDirection(new Vector3f(0.2F, 1.0F, -0.7F).normalize()),
						lightRotation.transformDirection(new Vector3f(-0.2F, 1.0F, 0.7F).normalize()));

				pose.mulPose(Axis.YP.rotationDegrees((float) yaw));
				pose.mulPose(Axis.XP.rotationDegrees((float) pitch));

				renderGun.renderInModTable(gun, pose, graphics.bufferSource(), LightTexture.FULL_BRIGHT);
				graphics.flush();
				pose.popPose();
				Lighting.setupFor3DItems();
			}
		}
	}
}
