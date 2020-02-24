package snorri.inventory;

import java.awt.Color;
import java.awt.Graphics;

import snorri.dialog.SpellMessage;
import snorri.dialog.TextMessage;
import snorri.entities.BossAIUnit;
import snorri.entities.Entity;
import snorri.events.CastEvent;
import snorri.events.CastEvent.Caster;
import snorri.main.Main;
import snorri.nonterminals.Sentence;
import snorri.windows.GameWindow;
import snorri.world.Vector;
import snorri.world.World;

public class Papyrus extends Item {

	private static final long serialVersionUID = 1L;
	private static final Color PAPYRUS_COOLDOWN_COLOR = new Color(118, 45, 50, 150);
	private static final double PAPYRUS_COOLDOWN = 1;
	
	private boolean ignoreMessages = false;
	private int numPapyri;
	
	// This event is used to saved a spell queued in the GUI.
	private CastEvent queuedCastEvent;
	
	public Papyrus(ItemType t) {
		super(t);
		timer = new Timer(PAPYRUS_COOLDOWN);
		numPapyri = 0;
	}
	
	public void addPapyri(int deltaPapyri) {
		numPapyri += deltaPapyri;
	}
	
	/** Returns true if papyri are removed. */
	public boolean removePapyri(int deltaPapyri) {
		int oldNumPapyri = numPapyri;
		numPapyri = Math.max(numPapyri - deltaPapyri, 0);
		return numPapyri != oldNumPapyri;
	}
	
	public boolean canCast() {
		return timer.isOffCooldown() && numPapyri > 0;
	}

	public void queueSpell(World world, Caster player, Vector castPos) {
		setSpell(null);
		((GameWindow) Main.getWindow()).openInventory(1);
		queuedCastEvent = new CastEvent(world, player, new Entity(castPos));
//		TODO: Should probably refactor this to remove Papyrus from the game completely.
//		if (numPapyri == 0 || !getTimer().isOffCooldown() || !(Main.getWindow() instanceof GameWindow)) {
//			return;
//		}
//		setSpell(null);
//		((GameWindow) Main.getWindow()).openInventory(1);
//		numPapyri--;
//		queuedCastEvent = new CastEvent(world, player, new Entity(castPos));
	}
	
	/**
	 * Cast a spell which has been set in the spellcrafting interface.
	 * @return False if the set spell is null or another error was encountered; true otherwise.
	 */
	public boolean checkQueuedSpell() {
		if (!(Main.getWindow() instanceof GameWindow) || queuedCastEvent == null || getSpell() == null) {
			return false;
		}
		GameWindow gameWindow = (GameWindow) Main.getWindow();
		Object spellResult = onCast(queuedCastEvent);
		String orthography = getSpell().getOrthography();
		gameWindow.showMessage(new SpellMessage(orthography, spellResult, spellIsStatement()));
		queuedCastEvent = null;
		return true;
	}
	
	/**
	 * This function has been repurposed for casting AI spells
	 * @param world
	 * @param caster The BossAIUnit caster
	 * @param subject
	 * @return true iff successfully activated
	 */
	public boolean castPrewritten(World world, BossAIUnit caster) {

		String orthography; //TODO calculate firstWord here?
		if (spell == null || "".equals(orthography = spell.getOrthography())) {
			if (!ignoreMessages && Main.getWindow() instanceof GameWindow) {
				((GameWindow) Main.getWindow()).showMessage(new TextMessage(null, "write on your papyrus before casting it!", false, new Runnable() {
					@Override
					public void run() {
						ignoreMessages = false;
					}
				}));
				ignoreMessages = true;
			}
			return false;
		}
		
		if (timer.activate()) {
			Object o = onCast(new CastEvent(world, caster, null));
			if (Main.getWindow() instanceof GameWindow) {
				((GameWindow) Main.getWindow()).showMessage(new SpellMessage(orthography, o, spellIsStatement()));
			}
			return true;
		}

		if (!ignoreMessages && Main.getWindow() instanceof GameWindow) {
			((GameWindow) Main.getWindow()).showMessage(new TextMessage(null, "papyrus is on cooldown!", false, new Runnable() {
				@Override
				public void run() {
					ignoreMessages = false;
				}
			}));
			ignoreMessages = true;
		}
		return false;

	}

	private boolean spellIsStatement() {
		return spell instanceof Sentence && ((Sentence) spell).isStatement();
	}

	@Override
	public Color getArcColor() {
		return PAPYRUS_COOLDOWN_COLOR;
	}

	@Override
	public int drawThumbnail(Graphics g, int i, boolean top, boolean selected) {
		int width = super.drawThumbnail(g, i, top, selected);
		Vector pos = getPos(i, top);
		g.drawString(String.valueOf(numPapyri), pos.getX(), pos.getY());
		return width;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(super.toString());
		builder.append(" (");
		builder.append(numPapyri);
		builder.append(")");
		return builder.toString();
	}

}