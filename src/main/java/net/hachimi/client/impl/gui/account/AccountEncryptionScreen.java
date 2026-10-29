package net.hachimi.client.impl.gui.account;

import net.hachimi.client.init.Managers;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class AccountEncryptionScreen extends Screen {
   private static final String SPECIAL_CHARACTERS = "!@#$%^&*()_+-={}[]\\|'\";:/?.>,<`";
   private static final String[] REQUIREMENTS = new String[]{"8+ Characters", "A Special Character", "A Number", "An Uppercase Letter"};
   private final Screen parent;
   private TextFieldWidget passwordTextField;

   public AccountEncryptionScreen(Screen parent) {
      super(Text.of("Account Encryption"));
      this.parent = parent;
   }

   @Override
   protected void init() {
      this.clearChildren();
      this.passwordTextField = new TextFieldWidget(this.client.textRenderer, 145, 20, Text.empty());
      this.passwordTextField.setPlaceholder(Text.of("Enter Password..."));
      this.passwordTextField.setPosition(this.width / 2 - this.passwordTextField.getWidth() / 2, this.height / 2 - 60);
      this.addDrawableChild(this.passwordTextField);
      this.addDrawableChild(
         ButtonWidget.builder(Text.of("Encrypt"), action -> {})
            .dimensions(this.width / 2 - 72, this.passwordTextField.getY() + 90, 145, 20)
            .tooltip(Tooltip.of(Text.of("This will require you to enter a password every time you enter the account manager the first time!")))
            .build()
      );
      this.addDrawableChild(
         ButtonWidget.builder(Text.of("Go Back"), action -> this.client.setScreen(this.parent))
            .dimensions(this.width / 2 - 72, this.passwordTextField.getY() + 112, 145, 20)
            .build()
      );
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredTextWithShadow(
         this.client.textRenderer, "Encrypt Accounts (" + Managers.ACCOUNT.getAccounts().size() + ")", this.width / 2, this.height / 2 - 125, -1
      );
      context.drawTextWithShadow(
         this.client.textRenderer,
         (this.isPasswordSecure(this.passwordTextField.getText()) ? Formatting.GREEN : Formatting.RED) + "*",
         this.width / 2 - this.passwordTextField.getWidth() / 2 - 6,
         this.passwordTextField.getY() + 10 - 9 / 2,
         -1
      );
      context.drawTextWithShadow(
         this.client.textRenderer,
         "Minimum Requirements:",
         this.passwordTextField.getX() + 1,
         this.passwordTextField.getY() + this.passwordTextField.getHeight() + 10,
         -1
      );

      for (int i = 0; i < REQUIREMENTS.length; i++) {
         String requirement = REQUIREMENTS[i];
         context.drawTextWithShadow(
            this.client.textRenderer,
            "- " + requirement,
            this.passwordTextField.getX() + 6,
            this.passwordTextField.getY() + this.passwordTextField.getHeight() + 10 + (9 + 2) * (1 + i),
            -1
         );
      }
   }

   private boolean isPasswordSecure(String password) {
      if (password.length() < 8) {
         return false;
      } else {
         boolean hasUppercase = false;
         boolean hasNumber = false;
         boolean hasSpecial = false;
         char[] characters = password.toCharArray();

         for (char c : characters) {
            if (Character.isUpperCase(c)) {
               hasUppercase = true;
            }

            if ("!@#$%^&*()_+-={}[]\\|'\";:/?.>,<`".indexOf(c) != -1) {
               hasSpecial = true;
            }

            if (c >= '0' && c <= '9') {
               hasNumber = true;
            }
         }

         return hasUppercase && hasNumber && hasSpecial;
      }
   }
}
