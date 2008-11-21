package control;

import io.GeneralPreferences;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Vector;

import model.MGraph;
import model.VEdge;
import model.VGraph;
import model.VNode;
/**
 * Standard Mouse Drag Hanlder extends the DragMouseHandler to the standard actions
 *
 * 
 * @author Ronny Bergmann
 *
 */
public class StandardDragMouseHandler extends DragMouseHandler
{

	private VGraph vg;
	private Point MouseOffSet;
	private GeneralPreferences gp;
	private VNode movingNode;
	private VEdge movingControlPointEdge;
	private int movingControlPointIndex, gridx,gridy;
	private boolean gridorientated, firstdrag;
	/**
	 * Initializes the Handler to a given VGraph
	 * 
	 * @param g the VGraph
	 */
	public StandardDragMouseHandler(VGraph g)
	{
		super(g);
		vg = g;
		gp = GeneralPreferences.getInstance();
		MouseOffSet = new Point(0,0);
		firstdrag = true; //Not dragged up to now
	}
	/**
	 * Returns the actual Startpoint if a drag is running, if not it returns 0,0
	 * 
	 * @return Startpoint of the drag action
	 */
	public Point getMouseOffSet()
	{
		return MouseOffSet;
	}
	/**
	 * Indicates, whether a drag is running or not
	 * 
	 * @return true, if a node or edge control point is moved (drag action is running)
	 */
	public boolean dragged()
	{
		return ((movingNode!=null)||(movingControlPointEdge!=null));
	}
	/** set whether the nodes are set to a gridpoint after dragging or not. 
	 * 
	 */
	public void setGridOrientated(boolean b)
	{
		gridorientated = b;
	}	
	/** update Gridinfo coordinate distances
	 * 
	 */
	public void setGrid(int x, int y)
	{
		gridx = x; gridy = y;
	}
	/**
	 * move edges that ar adjacent to a node, moves the edge control points
	 * - half the movement, if the adjacent node is selected
	 * @param nodeindex
	 * @param x
	 * @param y
	 */
	private void translateAdjacentEdges(int nodeindex, int x, int y)
	{
		x = Math.round(x/2);
		y = Math.round(y/2);
		Iterator<VEdge> edgeiter = vg.getEdgeIterator();
		while (edgeiter.hasNext())
		{
			VEdge e = edgeiter.next();
			int start = vg.getEdgeProperties(e.index).elementAt(MGraph.EDGESTARTINDEX);
			int ende = vg.getEdgeProperties(e.index).elementAt(MGraph.EDGEENDINDEX);
			if ((start==nodeindex)||(ende==nodeindex))
					e.translate(x, y);
		}
	}
	/**
	 * Handle a drag event, update the positions of moved nodes and theis adjacent edges
	 */
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		if (this.firstdrag)
			firstdrag = false; //First time really draged, so it's not firstdrag anymore
		if ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)
			return; //No ALT Handling here
		//Falls sich die Maus von Pressed bis Released bewegt hat...transalte alle selected Nodes
		//getX und getY heben dabei die Scrollposition auf die sonst mit MouseTempPos auch abgezogen wird.		
		Point p = new Point(e.getPoint().x-MouseOffSet.x, e.getPoint().y-MouseOffSet.y);
		//Bewegung des aktuellen objektes im Graphen (ohne Zoom
		int Gtransx = Math.round(p.x/((float)gp.getIntValue("vgraphic.zoom")/100));
		int Gtransy = Math.round(p.y/((float)gp.getIntValue("vgraphic.zoom")/100));
		//Position im Graphen (ohne Zoom)
		Point GPos = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100)));
		//Das ist die Bewegung p
		if ((((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))&&(movingNode!=null)) //shift n drag == Selection bewegen
		{
			Iterator<VNode> nodeiter = vg.getNodeIterator();
			while (nodeiter.hasNext()) // drawNodes
			{
				VNode temp = nodeiter.next();
				if (temp.isSelected())
				{
					Point newpoint = temp.getPosition(); //bisherige Knotenposition
					newpoint.translate(Gtransx,Gtransy); //Bewegung im Graphen aber mit Rungungsfehlern, also nur zurbetrachtung der Gesamtgraphbewegung
					if (newpoint.x < 0)
					{
						vg.translate(Math.abs(newpoint.x), 0); //Um die Differenz verschieben (Zoomfactor aufheben)
						newpoint.x=0;
					}
					if (newpoint.y < 0)
					{
						vg.translate(0,Math.abs(newpoint.y));
						newpoint.y = 0;
					}
					temp.setPosition(newpoint); //Translate selected node
					//move Adjacent Edges
					translateAdjacentEdges(temp.index, Gtransx,Gtransy);
				}
			}
			vg.pushNotify("NE");
		}
		else if ((InputEvent.ALT_DOWN_MASK & e.getModifiersEx()) == InputEvent.ALT_DOWN_MASK)
		{} //bei alt hier nichts tun
		else if (movingNode!=null) //ohne Shift ohne alt - gibt es einen Knoten r auf dem Mausposition, der nicht selektiert ist ?
		{
			Point newpos = movingNode.getPosition();
			newpos.translate(Gtransx,Gtransy); //Bewegung im Graphen aber mit Rungungsfehlern, also nur zurbetrachtung der Gesamtgraphbewegung
			if (newpos.x < 0)
			{
				vg.translate(Math.abs(newpos.x), 0);
				GPos.x=0;
			}
			if (newpos.y < 0)
			{
				vg.translate(0,Math.abs(newpos.y));
				GPos.y = 0;
			}
			translateAdjacentEdges(movingNode.index,Gtransx,Gtransy);	
			movingNode.setPosition(GPos);
			vg.pushNotify("N"); //Knoten aktualisiert
		}
		else if (movingControlPointEdge!=null)
		{
			Point newpos;
			Vector<Point> points = movingControlPointEdge.getControlPoints();
			newpos = points.get(movingControlPointIndex);
			newpos.translate(Gtransx,Gtransy); //Bewegung im Graphen aber mit Rungungsfehlern, also nur zurbetrachtung der Gesamtgraphbewegung
			if (newpos.x < 0)
			{
				vg.translate(Math.abs(newpos.x), 0);
				GPos.x=0;
			}
			if (newpos.y < 0)
			{
				vg.translate(0,Math.abs(newpos.y));
				GPos.y = 0;
			}
			points.set(movingControlPointIndex, GPos);
			movingControlPointEdge.setControlPoints(points);
			vg.pushNotify("E"); //Kanten aktualisiert
		}
		MouseOffSet = e.getPoint();
	}

	public void mouseMoved(MouseEvent arg0) {}

	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}
	/**
	 * If the mouse button is pressed initialize a drag (only if left mouse button is pressed)
	 */
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		firstdrag = true;
		MouseOffSet = e.getPoint(); //Aktuelle Position merken für eventuelle Bewegungen while pressed Zoom included
		Point p = new Point(Math.round(e.getPoint().x/((float)gp.getIntValue("vgraphic.zoom")/100)),Math.round(e.getPoint().y/((float)gp.getIntValue("vgraphic.zoom")/100))); //Rausrechnen des zooms
		if (!((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
		{
			movingNode = vg.getNodeinRange(p); //kein Shift == moving Node merken, sonst werden alle selected Bewegt
			if (gp.getBoolValue("vgraphic.cpshow")) 
			{
				Vector c = vg.getControlPointinRange(p, gp.getIntValue("vgraphic.cpsize"));
				if (c!=null)
				{
					movingControlPointEdge = (VEdge) c.get(0);
					movingControlPointIndex = ((Integer)c.get(1)).intValue();
				}
			}
		}
		else
		{//Shift - only handle stuff that beginns on a selected Node
			movingNode = vg.getNodeinRange(p);
			if ((movingNode!=null)&&(!movingNode.isSelected()))
			{
				movingNode=null; //do not start anything
				firstdrag = true;
			}
			VEdge selE = vg.getEdgeinRange(p, 2.0);
			if ((selE!=null)&&(selE.isSelected()))
			{ //Selected Edge, we move the selection so set the node to one of the edge adjacent nodes
				movingNode = vg.getNode(vg.getEdgeProperties(selE.index).get(MGraph.EDGESTARTINDEX)); 
			}
		}
	}
	/**
	 * ends the drag action, because the mouse button is released
	 */
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		if (!firstdrag) //If really dragged and not just clicked
		{
			if (!((e.getPoint().x==-1)||(e.getPoint().y==-1)))//kein Reset von außerhalb wegen modusumschaltung
				mouseDragged(e); //Das gleiche wie als wenn man bewegt, nur ist danach kein Knoten mehr bewegter Knoten
		}
		if (!((InputEvent.SHIFT_DOWN_MASK & e.getModifiersEx()) == InputEvent.SHIFT_DOWN_MASK))
		{ //No Shift - Snap the moving node (if existent) to grid
			if ((movingNode!=null)&&gridorientated) //nur dann zum grid snappen
			{ //nur ein Knoten wurde bewegt, snap to grid	
				
				int xdistanceupper = movingNode.getPosition().x%gridx;
				int xdistancelower = gridx-xdistanceupper;
				int ydistanceupper = movingNode.getPosition().y%gridy;
				int ydistancelower = gridy-ydistanceupper;
				Point newpos = new Point();
				if (xdistanceupper>xdistancelower) //näher am oberen der beiden werte
					newpos.x = (new Double(Math.ceil((double)movingNode.getPosition().x/(double)gridx)).intValue())*gridx;
				else 
					newpos.x =  (new Double(Math.floor((double)movingNode.getPosition().x/(double)gridx)).intValue())*gridx;
				if (ydistanceupper>ydistancelower) //näher am oberen der beiden werte
					newpos.y = (new Double(Math.ceil((double)movingNode.getPosition().y/(double)gridy)).intValue())*gridy;
				else 
					newpos.y =  (new Double(Math.floor((double)movingNode.getPosition().y/(double)gridy)).intValue())*gridy;
				movingNode.setPosition(newpos);	
			}
			movingNode=null;
			movingControlPointEdge=null;
			movingControlPointIndex = -1;
			firstdrag = true;
		}
	}

}
