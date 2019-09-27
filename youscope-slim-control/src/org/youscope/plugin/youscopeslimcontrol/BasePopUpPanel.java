package org.youscope.plugin.youscopeslimcontrol;

import java.awt.Dimension;
import javax.swing.JPanel;
import org.youscope.clientinterfaces.YouScopeClient;
import org.youscope.clientinterfaces.YouScopeFrame;

public class BasePopUpPanel extends JPanel
{   
	private static final long serialVersionUID = -7327196260576315134L;
	protected YouScopeFrame frame = null;
	private final YouScopeClient client;
	private String title;
	
	/**
	 * Constructor.
	 * @param client YouScope client.
	 */
	public BasePopUpPanel(YouScopeClient client) {
		setOpaque(true);
		this.client = client;

    }
	
	/**
	 * Convenient method to show this panel in a frame. The frame is not made visible and has a default title.
	 * @return The frame containing this panel.
	 */
	public YouScopeFrame toFrame()
	{
		if(frame != null)
			return frame;
		frame = client.createFrame();
		frame.setTitle(title);
		frame.setResizable(true);
		frame.setClosable(true);
		frame.setMaximizable(true);
		frame.setMargins(0,0,0,0);		
        frame.setContentPane(this);   
        frame.pack();
		return frame;
	}
	
	/**
	 * Sets the title of the frame, which is opened by {@link #toFrame()}. Has no effect when using this panel directly.
	 * @param title Title of the frame.
	 */
	public void setTitle(String title)
	{
		this.title = title;
		if(frame != null)
			frame.setTitle(title);
	}
	
	public void close() {
		if(frame != null) {
			frame.close();
		}
	}
	
	public void setSize(Dimension size) {
		if(frame != null) {
			frame.setSize(size);
		}
	}
}