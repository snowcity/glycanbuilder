/*
*   EuroCarbDB, a framework for carbohydrate bioinformatics
*
*   Copyright (c) 2006-2011, Eurocarb project, or third-party contributors as
*   indicated by the @author tags or express copyright attribution
*   statements applied by the authors.  
*
*   This copyrighted material is made available to anyone wishing to use, modify,
*   copy, or redistribute it subject to the terms and conditions of the GNU
*   Lesser General Public License, as published by the Free Software Foundation.
*   A copy of this license accompanies this distribution in the file LICENSE.txt.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
*   or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
*   for more details.
*   
*   @author David R. Damerell (david@nixbioinf.org)
*/
package ac.uk.icl.dell.vaadin.glycanbuilder;

import static org.eurocarbdb.application.glycanbuilder.Geometry.center;
import static org.eurocarbdb.application.glycanbuilder.Geometry.distance;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Transferable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import org.eurocarbdb.application.glycanbuilder.BBoxManager;
import org.eurocarbdb.application.glycanbuilder.BaseDocument.DocumentChangeEvent;
import org.eurocarbdb.application.glycanbuilder.BaseDocument.DocumentChangeListener;
import org.eurocarbdb.application.glycanbuilder.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanDocument;
import org.eurocarbdb.application.glycanbuilder.GlycanRenderer;
import org.eurocarbdb.application.glycanbuilder.GlycanRendererMode;
import org.eurocarbdb.application.glycanbuilder.GlycanSelection;
import org.eurocarbdb.application.glycanbuilder.GraphicOptions;
import org.eurocarbdb.application.glycanbuilder.Linkage;
import org.eurocarbdb.application.glycanbuilder.LogUtils;
import org.eurocarbdb.application.glycanbuilder.Paintable;
import org.eurocarbdb.application.glycanbuilder.PositionManager;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.ResidueSelectorDialog;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.SVGUtils;
import org.eurocarbdb.application.glycanbuilder.TerminalDictionary;
import org.eurocarbdb.application.glycanbuilder.TextUtils;

public class GlycanCanvas implements DocumentChangeListener{
	protected HashMap<Rectangle,Residue> boundingBoxes=new HashMap<Rectangle,Residue>();
	protected BBoxManager theBBoxManager;
	protected PositionManager posManager;
	
	protected HashSet<Linkage> selectedLinkages;
	protected HashSet<Residue> selectedResidues;
	
	protected GlycanRenderer theGlycanRenderer;
	
	protected org.eurocarbdb.application.glycanbuilder.BuilderWorkspace theWorkspace;
	protected org.eurocarbdb.application.glycanbuilder.GlycanDocument theDoc;

	protected HashSet<String> residueTypeHistory=new HashSet<String>();
	protected Vector<String> residueTypeHistoryList=new Vector<String>();
	
	protected HashSet<ResidueHistoryListener> residueHistoryListeners=new HashSet<ResidueHistoryListener>();
	
	@SuppressWarnings("unused")
	private Collection<Glycan> sel;
	@SuppressWarnings("unused")
	private Linkage currentLinkage;
	
	private Residue currentResidue;
	
	private boolean pauseSelectionUpdates=false;
	
	
	public org.eurocarbdb.application.glycanbuilder.GlycanDocument getTheDoc() {
		return theDoc;
	}

	protected Paintable thePaintable;
	
	protected int height;
	
	@SuppressWarnings("unused")
	private boolean residueSelected=false;
	
	private boolean documentChangedEventFiring=true;
	
	public boolean isDocumentChangedEventFiring() {
		return documentChangedEventFiring;
	}

	public void setDocumentChangedEventFiring(boolean documentChangedEventFiring) {
		this.documentChangedEventFiring = documentChangedEventFiring;
	}

	public int getHeight() {
		return height;
	}

	protected HashSet<GlycanCanvasUpdateListener> glycanCanvasUpdateListeners;
	
	public interface GlycanCanvasUpdateListener {
		public void glycanCanvasUpdated();
	}
	
	public void fireGlycanCanvasUpdated(){
		if(documentChangedEventFiring){
			for(GlycanCanvasUpdateListener glycanCanvasListener:glycanCanvasUpdateListeners){
				glycanCanvasListener.glycanCanvasUpdated();
			}
		}
	}
	
	public void addGlycanCanvasUpdateListener(GlycanCanvasUpdateListener glycanCanvasUpdateListener){
		glycanCanvasUpdateListeners.add(glycanCanvasUpdateListener);
	}
	
	public GlycanCanvas(GlycanRenderer glycanRenderer,Paintable paintable) {
		thePaintable=paintable;
		
		theWorkspace=new org.eurocarbdb.application.glycanbuilder.BuilderWorkspace(glycanRenderer);
		
		theGlycanRenderer=theWorkspace.getGlycanRenderer();
		
		posManager = new PositionManager();
		theBBoxManager = new BBoxManager();
		
		selectedResidues=new HashSet<Residue>();
		selectedLinkages=new HashSet<Linkage>();
		
		theDoc=theWorkspace.getStructures();
		
		theDoc.addDocumentChangeListener(this);
		
		glycanCanvasUpdateListeners=new HashSet<GlycanCanvasUpdateListener>();
	}
	
	public void setNotation(String notation) {
		theWorkspace.setNotation(notation);
		documentUpdated();
		fireNotationChanged();
	}
	
	public void addSequenceByName(String name){
		try {
			theDoc.addStructure(CoreDictionary.newCore(name));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addResidueByNameToSelected(String name){
		if(theGlycanRenderer.getRenderMode()==GlycanRendererMode.TOOLBAR || selectedResidues.size()==0){
			try{
				Residue toAdd = ResidueDictionary.newResidue(name);
				theDoc.addResidue(null, getLinkedResidues(),toAdd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else{
			for(Residue selectedResidue:selectedResidues){
				try {
					Residue toAdd = ResidueDictionary.newResidue(name);
					if (theDoc.addResidue(selectedResidue, getLinkedResidues(),toAdd) != null) {
						setSelection(toAdd);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		addResidueToHistory(name);
	}
	
	public void changeSelectedToResidueByName(String name){
		boolean changed=false;
		for(Residue selectedResidue:selectedResidues){
			try{
				ResidueType newType = ResidueDictionary.getResidueType(name);
				if (theDoc.changeResidueType(selectedResidue, getLinkedResidues(), newType)) {
					//theWorkspace.getResidueHistory().add(current);
					changed=true;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		if(changed){
			addResidueToHistory(name);
		}
	}
	
	public void insertResidueByNameBeforeSelected(String name){
		boolean inserted=false;
		for(Residue selectedResidue:selectedResidues){
			System.err.println("AI: "+selectedResidue.getTypeName());
			try{
				Residue toInsert = ResidueDictionary.newResidue(name);
				if (theDoc.insertResidueBefore(selectedResidue, getLinkedResidues(),toInsert) != null) {
					inserted=true;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		if(inserted){
			addResidueToHistory(name);
		}
	}
	
	public String[] toStrings(char[] pos) {
		String[] ret = new String[pos.length];
		for (int i = 0; i < pos.length; i++)
			ret[i] = "" + pos[i];
		return ret;
	}
	
	public List<String> createPositions(Residue parent) {
		List<String> ret = new ArrayList<String>();

		// collect available positions
		char[] par_pos = null;
		if (parent == null
				|| parent.getType().getLinkagePositions().length == 0)
			par_pos = new char[] { '1', '2', '3', '4', '5', '6', '7', '8', '9',
					'N' };
		else
			par_pos = parent.getType().getLinkagePositions();

		// add elements
		ret.add("?");
		for (int i = 0; i < par_pos.length; i++)
			ret.add("" + par_pos[i]);

		return ret;
	}
	
	public void resetSelection(){
		selectedResidues.clear();
		selectedLinkages.clear();
		currentResidue = null;
		currentLinkage = null;
		fireUpdatedSelection();
	}

	public void setSelection(Linkage link) {
		pauseSelectionUpdates=true;
		resetSelection();
		if(link!=null){
			selectedLinkages.add(link);
			currentLinkage = link;
		}
		pauseSelectionUpdates=false;
		fireUpdatedSelection();
	}

	public void setSelection(Residue node) {
		pauseSelectionUpdates=true;
		resetSelection();
		if(node!=null){
			selectedResidues.add(node);
			selectedResidues.addAll(theBBoxManager.getLinkedResidues(node));
			currentResidue = node;
		}	
		pauseSelectionUpdates=false;
		fireUpdatedSelection();
	}
	
	public void documentUpdated(){
		thePaintable.clear();
		
		posManager = new PositionManager();
		theBBoxManager = new BBoxManager();
		
		if(!theDoc.isEmpty()){
			theGlycanRenderer.computeBoundingBoxes(theDoc.getStructures(), theWorkspace.getGraphicOptions().SHOW_MASSES_CANVAS,theWorkspace.getGraphicOptions().SHOW_REDEND_CANVAS, posManager, theBBoxManager);
		
			for(Glycan glycan:theDoc.getStructures()){
				theGlycanRenderer.paint(thePaintable,glycan, selectedResidues, selectedLinkages, theWorkspace.getGraphicOptions().SHOW_MASSES_CANVAS,theWorkspace.getGraphicOptions().SHOW_REDEND_CANVAS, posManager, theBBoxManager);
			}
		}
		
		updateCanvasHeight();
		
		fireGlycanCanvasUpdated();
	}


	@Override
	public void documentInit(DocumentChangeEvent e) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void documentChanged(DocumentChangeEvent e) {
		if(documentChangedEventFiring){
			documentUpdated();
		}
	}
	
	
	
	/**
	 * Return the residue with the focus
	 */
	public Residue getCurrentResidue() {
		return currentResidue;
	}

	/**
	 * Return all the residues that are shown at the same position of the
	 * residue with the focus
	 */
	public Vector<Residue> getLinkedResidues() {
		return theBBoxManager.getLinkedResidues(getCurrentResidue());
	}
	
	/**
	 * Delete the selected residues/structures and copy them to the clipboard
	 */
//	public void cut() {
//		copy();
//		delete();
//	}

	
	Transferable clipBoard;
	
	/**
	 * Delete the selected residues/structures and copy them to the clipboard
	 */
	public void cut() {
		copy();
		delete();
	}

	/**
	 * Copy the selected residues/structures to the clipboard
	 */
	public void copy() {
		Collection<Glycan> sel = theDoc.extractView(selectedResidues);
		
		clipBoard=new GlycanSelection(theGlycanRenderer, sel);
	}

	/**
	 * Copy all selected structures to the clipboard
	 */
	public void copySelectedStructures() {
//		ClipUtils.setContents(new GlycanSelection(getTheGlycanRenderer(),
//				getSelectedStructures()));
	}

	/**
	 * Copy all structures to the clipboard
	 */
	public void copyAllStructures() {
//		ClipUtils.setContents(new GlycanSelection(getTheGlycanRenderer(), theDoc
//				.getStructures()));
	}

	/**
	 * Paste the content of the clipboard into the editor
	 */
	public void paste() {
		try {
			Transferable t = clipBoard;
			if (t != null
					&& t.isDataFlavorSupported(GlycanSelection.glycoFlavor)) {
				String content = TextUtils.consume((InputStream) t.getTransferData(GlycanSelection.glycoFlavor));
				for(Residue selectedResidue:selectedResidues){
					theDoc.addStructures(selectedResidue, theDoc
							.parseString(content));
				}
				
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Delete the selected residues/structures
	 */
	public void delete() {
		@SuppressWarnings("unchecked")
		HashSet<Residue> shallowClone=(HashSet<Residue>) selectedResidues.clone();
		
		selectedResidues.clear();
		theDoc.removeResidues(shallowClone);
	}
	
	/**
	 * Return the linkage displayed at the specified position, or
	 * <code>null</code> if none is there
	 */
	public Linkage getLinkageAtPoint(Point p) {
		for (Glycan g : theDoc.getStructures()) {
			Linkage ret = getLinkageAtPoint(g.getRoot(), p);
			if (ret != null){
				return ret;
			}

			ret = getLinkageAtPoint(g.getBracket(), p);
			if (ret != null){
				return ret;
			}
		}
		
		System.err.println("No linkage selected!");
		return null;
	}
	
	/**
	 * Return the linkage from a residue that is displayed at the specified
	 * position, or <code>null</code> if none is there
	 * 
	 * @param r
	 *            the parent
	 */
	public Linkage getLinkageAtPoint(Residue r, Point p) {
		if (r == null)
			return null;

		Rectangle cur_bbox = theBBoxManager.getCurrent(r);
		for (Linkage l : r.getChildrenLinkages()) {
			Rectangle child_bbox = theBBoxManager.getCurrent(l
					.getChildResidue());
			if (cur_bbox != null && child_bbox != null
					&& distance(p, center(cur_bbox), center(child_bbox)) < 4.)
				return l;

			Linkage ret = getLinkageAtPoint(l.getChildResidue(), p);
			if (ret != null)
				return ret;
		}
		return null;
	}
	
	public boolean hasSelected(){
		return selectedResidues.size()>0 || selectedLinkages.size()>0 ? true : false;
	}
	
	public boolean hasSelectedResidues(){
		return selectedResidues.size()>0 ? true : false;
	}
	
	public void selectIntersectingRectangles(double x, double y, double width,double height,boolean mouseMoved){
		pauseSelectionUpdates=true;
		
		boolean hadSelected=hasSelected();
		
		List<Residue> selected=new ArrayList<Residue>();
		for(Residue residue:theBBoxManager.current_bboxes.keySet()){
			
			
			Rectangle rec=theBBoxManager.current_bboxes.get(residue);
			
			if(mouseMoved){
				if(rec.y < y+height && rec.y+rec.height > y && rec.x < x+width && rec.x+rec.width > x){
					selected.add(residue);
					selected.addAll(theBBoxManager.getLinkedResidues(residue));
				}
			}else{
				if(x>=rec.x && x<=rec.x+rec.width && y>=rec.y && y<=rec.y+rec.height){
					selected.add(residue);
					selected.addAll(theBBoxManager.getLinkedResidues(residue));
				}
			}
		}
		
		if(selected.size()==1){
			setSelection(selected.get(0));
		}else{
			setSelection(selected);
		}
		
		if(!hasSelected()){
			Linkage link = getLinkageAtPoint(new Point((int)x,(int)y));
			setSelection(link);
		}
		
		pauseSelectionUpdates=false;
		
		if(hadSelected==false && hasSelected()==false){
			return;
		}else{
			fireUpdatedSelection();
		}
	}
	
	public void setSelection(Collection<Residue> nodes) {
		resetSelection();
		if(nodes != null && !nodes.isEmpty()){
			for (Residue node : nodes) {
				selectedResidues.add(node);
				selectedResidues.addAll(theBBoxManager.getLinkedResidues(node));
			}
			
			fireUpdatedSelection();
		}
	}
	
	public interface SelectionChangeListener {
		public void selectionChanged();
	}
	
	private HashSet<SelectionChangeListener> selectionChangeListeners=new HashSet<SelectionChangeListener>();
	
	public void addSelectionChangeListener(SelectionChangeListener listener){
		selectionChangeListeners.add(listener);
	}
	
	public void removeSelectionChangeListener(SelectionChangeListener listener){
		selectionChangeListeners.remove(listener);
	}
	
	public void fireUpdatedSelection(){
		if(!pauseSelectionUpdates){
			documentUpdated();
			
			for(SelectionChangeListener listener:selectionChangeListeners){
				listener.selectionChanged();
			}
		}
	}
	
	public void selectAllResiduesByName(HashSet<String> residueNames){
		for(String residueName:residueNames){
			selectAllResiduesByName(residueName);
		}
	}
	
	public void selectAllResiduesByName(String name){
		for(Glycan glycan:theDoc.getStructures()){
			for(Residue residue:glycan.getAllResidues()){
				if(residue.getTypeName().equals(name)){
					selectedResidues.add(residue);
				}
			}
		}
	}
	
	private void updateCanvasHeight(){
		height=-1;
		for(Residue residue:theBBoxManager.complete_bboxes.keySet()){
			Rectangle rec=theBBoxManager.complete_bboxes.get(residue);
			if(rec.y+rec.height > height){
				height=rec.y+rec.height;
			}
		}
		
		height=height+theWorkspace.getGraphicOptions().MARGIN_TOP+theWorkspace.getGraphicOptions().MARGIN_BOTTOM+theWorkspace.getGraphicOptions().MASS_TEXT_SPACE;
	}
	
	public interface ResidueHistoryListener{
		public void respondToResidueHistoryChanged(Vector<String> residueHistory);
	}
	
	public void addResidueToHistory(String typeName){
		if(theGlycanRenderer.getRenderMode()==GlycanRendererMode.DRAWING && !residueTypeHistory.contains(typeName)){
			int maxResidues=5;
			if(residueTypeHistoryList.size() >maxResidues){
				residueTypeHistory.remove(residueTypeHistoryList.remove(0));
			}
			
			residueTypeHistory.add(typeName);
			
			residueTypeHistoryList.add(typeName);
			
			fireResidueHistoryChanged();
		}
	}
	
	protected void fireResidueHistoryChanged(){
		for(ResidueHistoryListener listener:residueHistoryListeners){
			listener.respondToResidueHistoryChanged(residueTypeHistoryList);
		}
	}
	
	public void addResidueHistoryListener(ResidueHistoryListener listener){
		residueHistoryListeners.add(listener);
	}
	
	public void removeResidueHistoryListener(ResidueHistoryListener listener){
		residueHistoryListeners.remove(listener);
	}
	
	public void clear(){
		theDoc.clear();
		selectedResidues.clear();
		selectedLinkages.clear();
	}
	
	public String getOrientationIcon(){
		if(theWorkspace.getGraphicOptions().ORIENTATION==GraphicOptions.BT){
			return "bt.png";
		}else if(theWorkspace.getGraphicOptions().ORIENTATION==GraphicOptions.LR){
			return "lr.png";
		}else if(theWorkspace.getGraphicOptions().ORIENTATION==GraphicOptions.RL){
			return "rl.png";
		}else{
			return "tb.png";
		}
	}
	
	/**
	 * Change the orientation of the display
	 */
	public void changeOrientation() {
		theWorkspace.getGraphicOptions().ORIENTATION = (theWorkspace.getGraphicOptions().ORIENTATION + 1) % 4;
		documentUpdated();
	}
	
	HashSet<NotationChangedListener> notationChangeListeners=new HashSet<NotationChangedListener>();
	
	public interface NotationChangedListener {
		public void notationChanged(String notation);
	}
	
	public void addNotationListener(NotationChangedListener listener){
		notationChangeListeners.add(listener);
	}
	
	public void removeNotationListener(NotationChangedListener listener){
		notationChangeListeners.remove(listener);
	}
	
	public void fireNotationChanged(){
		for(NotationChangedListener  listener:notationChangeListeners){
			listener.notationChanged(theWorkspace.getGraphicOptions().NOTATION);
		}
	}
	
	public void selectAll() {
		for (Iterator<Glycan> i = theDoc.iterator(); i.hasNext();) {
			Glycan structure = i.next();
			selectAll(structure.getRoot());
			selectAll(structure.getBracket());
		}
		
		selectedLinkages.clear();
		currentResidue = null;
		currentLinkage = null;

		if (theDoc.getFirstStructure() != null){
			currentResidue = theDoc.getFirstStructure().getRoot();
		}
	}
	
	private void selectAll(Residue node) {
		if (node == null){
			return;
		}

		selectedResidues.add(node);
		for (Iterator<Linkage> i = node.iterator(); i.hasNext();){
			selectAll(i.next().getChildResidue());
		}
	}
	
	public String getInternalFormat(String longFormat){
		for(java.util.Map.Entry<String,String> e : GlycanDocument.getExportFormats().entrySet()){
			if(longFormat.equals(e.getValue())){
				return e.getKey();
			}
		}
		
		return null;
	}
	
	public List<String> getSequenceExportFormats(){
		List<String> exportFormats=new ArrayList<String>();
		
		for(java.util.Map.Entry<String,String> e : GlycanDocument.getExportFormats().entrySet()){
			exportFormats.add(e.getValue());
		}
		
		return exportFormats;
	}
	
	public String getImageExportShortFormat(String longFormat){
		for(Entry<String, String> e : SVGUtils.getExportFormats().entrySet()){
			if(longFormat.equals(e.getValue())){
				return e.getKey();
			}
		}
		
		return null;
	}
	
	public List<String> getImageExportFormats(){
		List<String> exportFormats=new ArrayList<String>();
		
		for(Entry<String, String> e : SVGUtils.getExportFormats().entrySet()){
			exportFormats.add(e.getValue());
		}
		
		return exportFormats;
	}
	
	
	public List<String> getImportFormats(){
		List<String> importFormats=new ArrayList<String>();
		for(java.util.Map.Entry<String,String> e : GlycanDocument.getImportFormats().entrySet()){
			importFormats.add(e.getValue());
		}
		
		return importFormats;
	}
	
	public String getImportFormatShortFormat(String longFormat){
		for(java.util.Map.Entry<String,String> e : GlycanDocument.getImportFormats().entrySet()){
			if(longFormat.equals(e.getValue())){
				return e.getKey();
			}
		}
		
		return null;
	}
	
	/**
	 * Add a bracket residue to the structure with the focus
	 */
	public void addBracket() {
		Residue bracket = theDoc.addBracket(getCurrentResidue());
		if (bracket != null){
			setSelection(bracket);
			documentUpdated();
		}
	}
	
	/**
	 * Add a new structure from a terminal motif
	 * 
	 * @param name
	 *            the identifier of the terminal motif
	 * @see TerminalDictionary
	 */
	public void addTerminal(String name) {
		try {
			Residue toadd = TerminalDictionary.newTerminal(name);
			Residue current = getCurrentResidue();
			if(theDoc.addResidue(current, getLinkedResidues(), toadd) != null){
				setSelection(current);
			}
			
			documentUpdated();
		} catch (Exception e) {
			LogUtils.report(e);
		}
	}
	
	/**
	 * Return all the structures containing the selected residues and linkages
	 */
	public Collection<Glycan> getSelectedStructures() {
		return theDoc.findStructuresWith(selectedResidues, selectedLinkages);
	}
	
	/**
	 * Return a list containing the selected residues
	 */
	public Collection<Residue> getSelectedResiduesList() {
		return selectedResidues;
	}
	
	public GlycanRenderer getTheGlycanRenderer() {
		return theGlycanRenderer;
	}
	
	/**
	 * Add a repeating unit containing the selected residues. If the end point
	 * of the unit cannot be easily determined a {@link ResidueSelectorDialog}
	 * is shown
	 */
	public void addRepeat() throws Exception{
		Collection<Residue> nodes = getSelectedResiduesList();
		if (!theDoc.createRepetition(null, nodes)) {
			Vector<Residue> end_points = new Vector<Residue>();
			for(Residue r : nodes){
				if(r.isSaccharide()){
					end_points.add(r);
				}
			}
			
			//Glycan structure = getSelectedStructures().iterator().next();
			//TODO
			//ResidueSelectorDialog rsd = new ResidueSelectorDialog(
			//		theParent, "Select ending point",
			//		"Select ending point of the repetition", structure,
			//		end_points, false, getTheGlycanRenderer());

			//rsd.setVisible(true);
			//if (!rsd.isCanceled())
				//theDoc.createRepetition(rsd.getCurrentResidue(),
					//	getSelectedResiduesList());
		}
		
		documentUpdated();
	}
}