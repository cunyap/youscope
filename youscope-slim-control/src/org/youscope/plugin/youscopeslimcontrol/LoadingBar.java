package org.youscope.plugin.youscopeslimcontrol;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.OverlayLayout;
import javax.swing.UIManager;

import org.youscope.clientinterfaces.YouScopeClient;

public class LoadingBar extends BasePopUpPanel {
	JProgressBar progressBar;
	JPanel backgroundPanel = new JPanel();
	public LoadingBar(YouScopeClient client, String title) {
		super(client);
		//backgroundPanel.setLayout(new BoxLayout(backgroundPanel, BoxLayout.LINE_AXIS));
		backgroundPanel.setLayout(new OverlayLayout(backgroundPanel));
		setLook(title);
		setLoadingBar();

		super.add(backgroundPanel);
	}
	public void setLook(String title) {
		setSize(100,50);
		setTitle(title);
		JLabel label = new JLabel(new ImageIcon("Icons/progressBarIcon3.png"));
		backgroundPanel.add(label, LEFT_ALIGNMENT);
	}

	private void setLoadingBar() {
		progressBar = new JProgressBar(0,100);
		backgroundPanel.add(progressBar);
	}

	public void setValue(int n) {
		progressBar.setValue(n);
	}

	public void setForegroundBar(Color color) {
		progressBar.setStringPainted(true);
		progressBar.setForeground(color);
		progressBar.setString("");

	}
}
