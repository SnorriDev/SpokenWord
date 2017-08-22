package snorri.dialog;

import java.awt.Image;
import java.io.Serializable;

import javax.swing.ImageIcon;

import snorri.main.GameWindow;
import snorri.main.Main;

/**
 * Wrapper class to read dialog and other HTML text from YAML file
 */

public class Dialog implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	/**Name that will appear in bold under portrait*/
	public String name = "";
	/**HTML text to show*/
	public String text = "";
	/**Image for actual dialog*/
	public String image = null;
	public boolean showObjective = false;
		
	public Dialog() {
	}
	
	// will probably reuse a lot of portraits
	// could still unify this with getImage
	public String getName() {
		return name;
	}
	
	public Image getImage() {
		return Portraits.get(image);
	}

	public ImageIcon getIcon() {
		return new ImageIcon(getImage());
	}

	public Dialog addObjective(boolean flag) {
		
		if (!flag || !(Main.getWindow() instanceof GameWindow)) {
			return this;
		}
		
		Dialog newDialog = new Dialog();
		newDialog.text = text + "\n" + ((GameWindow) Main.getWindow()).getObjectiveInfo();
		newDialog.image = image;
		return newDialog;
		
	}
		
}