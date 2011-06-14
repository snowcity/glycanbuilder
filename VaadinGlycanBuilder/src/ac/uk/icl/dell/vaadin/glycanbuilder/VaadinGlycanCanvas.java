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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.eurocarbdb.application.glycanbuilder.AbstractGlycanRenderer;
import org.eurocarbdb.application.glycanbuilder.CoreDictionary;
import org.eurocarbdb.application.glycanbuilder.CoreType;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.GlycanRendererMode;
import org.eurocarbdb.application.glycanbuilder.GraphicOptions;
import org.eurocarbdb.application.glycanbuilder.Linkage;
import org.eurocarbdb.application.glycanbuilder.LogUtils;
import org.eurocarbdb.application.glycanbuilder.MassOptions;
import org.eurocarbdb.application.glycanbuilder.RepetitionPropertiesDialog;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.ResiduePropertiesDialog;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.SVGUtils;
import org.eurocarbdb.application.glycanbuilder.TerminalDictionary;
import org.eurocarbdb.application.glycanbuilder.TerminalType;
import org.vaadin.damerell.canvas.BasicCanvas;
import org.vaadin.damerell.canvas.font.Font;
import org.vaadin.damerell.canvas.font.Font.FONT;
import org.vaadin.hene.popupbutton.PopupButton;
import org.vaadin.navigator7.NavigableApplication;

import ac.uk.icl.dell.vaadin.LocalResourceWatcher;
import ac.uk.icl.dell.vaadin.MessageDialogBox;
import ac.uk.icl.dell.vaadin.ProducesLocalResources;
import ac.uk.icl.dell.vaadin.glycanbuilder.MassOptionsDialog.MassOptionListener;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.terminal.FileResource;
import com.vaadin.terminal.ThemeResource;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FailedEvent;
import com.vaadin.ui.Upload.SucceededEvent;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class VaadinGlycanCanvas extends BasicCanvas implements BasicCanvas.SelectionListener, GlycanCanvas.GlycanCanvasUpdateListener, GlycanCanvas.SelectionChangeListener, ProducesLocalResources, MassOptionListener{
	private static final long serialVersionUID=-4055030966241059255L;
	
	private List<Component> componentsWithResidueSelectionDependency;
	public List<MenuBar.MenuItem> menuItemsWithResidueSelectionDependency;
	
	private List<LocalResourceWatcher> localResourceWatchers=new ArrayList<LocalResourceWatcher>();
	
	//private Panel theParentPanel;
	
	protected GlycanCanvas theCanvas;
	
	protected boolean automaticallyAdjustHeight=true;

	private OptionGroup field_anomeric_state;

	private OptionGroup field_anomeric_carbon;

	private OptionGroup field_linkage_position;

	private OptionGroup field_chirality;

	private OptionGroup field_ring_size;

	private CheckBox field_second_bond;

	private OptionGroup field_second_child_position;

	private OptionGroup field_second_parent_position;

	private PopupButton linkage_one_panel;

	private PopupButton linkage_two_panel;

	public VaadinGlycanCanvas() {
		theCanvas=new GlycanCanvas(new GlycanRendererCanvas(),new CanvasPaintable(this));
		theCanvas.addGlycanCanvasUpdateListener(this);
		
		this.addListener((CanvasMouseUpListener)this);
		this.addListener((CanvasMouseMoveListener)this);
		this.addListener((CanvasMouseDownListener)this);
		
		font=Font.getFont(FONT.STANDARD);
		
		componentsWithResidueSelectionDependency=new ArrayList<Component>();
		menuItemsWithResidueSelectionDependency=new ArrayList<MenuBar.MenuItem>();
		
	//	theParentPanel=parent;
		
		addSelectionListener(this);
		
		theCanvas.addSelectionChangeListener(this);
		theCanvas.theWorkspace.getGraphicOptions().SHOW_REDEND_CANVAS=false;
		theCanvas.theWorkspace.getGraphicOptions().SHOW_MASSES_CANVAS=false;
	}

	public void appendStructureMenu(MenuBar.MenuItem parent){
		MenuBar.MenuItem structureMenu=parent.addItem("Add structure", null);

		for(String superclass:CoreDictionary.getSuperclasses()){
			MenuBar.MenuItem superClassMenu=structureMenu.addItem(superclass,null);
			for(final CoreType t:CoreDictionary.getCores(superclass)){
				superClassMenu.addItem(t.getName(), new Command(){
					/**
					 * 
					 */
					private static final long serialVersionUID=-148395291969656325L;

					@Override
					public void menuSelected(MenuItem selectedItem) {
						theCanvas.addSequenceByName(t.getName());
					}
				});
			}
		}
		
		createAddResidueMenu(parent);
		createInsertResidueMenu(parent);
		createChangeResidueMenu(parent);
		createTerminalMenu(parent);
	}
	
	private void createTerminalMenu(MenuBar.MenuItem parent){
		MenuBar.MenuItem structureMenu=parent.addItem("Add terminal", null);
		for(String superclass : TerminalDictionary.getSuperclasses()){
			MenuBar.MenuItem superClassMenu=structureMenu.addItem(superclass,null);
			for (TerminalType t : TerminalDictionary.getTerminals(superclass)) {
				MenuBar.MenuItem terminalType=superClassMenu.addItem(t.getDescription(),null);
				
				final String type=t.getName();
				Command addTerminal=new Command(){

					/**
					 * 
					 */
					private static final long serialVersionUID=6872577027235164629L;

					@Override
					public void menuSelected(MenuItem selectedItem) {
						String longForm=selectedItem.getText();
						
						if(longForm.equals("Unknown linkage")){
							theCanvas.addTerminal(type);
						}else{
							theCanvas.addTerminal(longForm.charAt(0)+"-"+type);
						}
					}
				};
				
				terminalType.addItem("Unknown linkage", addTerminal);
				for (int l = 1; l < 9; l++) {
					terminalType.addItem(l+"-linked", addTerminal);
				}
			}
		}
	}
	
	public void appendImportMenu(MenuBar.MenuItem parent){
		final Window importWindow=new Window(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-397373017493034496L;

			public void close(){
				setVisible(false);
			}
		};
		importWindow.setCaption("Import from sequence");
		importWindow.setWidth("400px");
		
		final CanvasImport canvasImport=new CanvasImport();
		importWindow.getContent().addComponent(canvasImport);
		importWindow.center();
		importWindow.setVisible(false);
		
		@SuppressWarnings("unused")
		MenuBar.MenuItem importMenu=parent.addItem("Import", new Command(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-6735134306275926140L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				if(!importWindow.isVisible()){
					if(getWindow().getChildWindows().contains(importWindow)==false){
						getWindow().addWindow(importWindow);
					}
					
					importWindow.setVisible(true);
				}
			}
		});
		
		final Window importFromStringWindow=new Window(){
			/**
			 * 
			 */
			private static final long serialVersionUID=7035248961169308096L;

			public void close(){
				setVisible(false);
			}
		};
		importFromStringWindow.setCaption("Import sequence from string");
		importFromStringWindow.setWidth("400px");
		
		final ImportStructureFromStringDialog importStructureStringDialog=new ImportStructureFromStringDialog(theCanvas);
		
		importStructureStringDialog.addListener(new UserInputEndedListener() {
			@Override
			public void done(boolean cancelled) {
				if(!cancelled){
					System.err.println(">");
					System.err.println(importStructureStringDialog.getSequenceString());
					System.err.println("|");
					if(!theCanvas.theDoc.importFromString(importStructureStringDialog.getSequenceString(),theCanvas.getImportFormatShortFormat(importStructureStringDialog.getSequenceFormat()))){
						MessageDialogBox box=new MessageDialogBox("String import error", LogUtils.getLastError(), NavigableApplication.getCurrentNavigableAppLevelWindow());
						box.center();
						box.setVisible(true);
					}
				}
			}
		});
		
		importFromStringWindow.getContent().addComponent(importStructureStringDialog);
		((VerticalLayout)importFromStringWindow.getContent()).setComponentAlignment(importStructureStringDialog, Alignment.MIDDLE_CENTER);
		importFromStringWindow.center();
		importFromStringWindow.setVisible(false);
		
		
		@SuppressWarnings("unused")
		MenuBar.MenuItem importFromStringMenu=parent.addItem("Import from string", new Command(){
			/**
			 * 
			 */
			private static final long serialVersionUID=1586089744665899803L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				if(!importFromStringWindow.isVisible()){
					if(getWindow().getChildWindows().contains(importFromStringWindow)==false){
						getWindow().addWindow(importFromStringWindow);
					}
					
					importFromStringWindow.setVisible(true);
				}
			}
		});
		
		
	}
	
	public class CanvasImport extends Panel implements Upload.SucceededListener,Upload.FailedListener,Upload.Receiver{
		/**
		 * 
		 */
		private static final long serialVersionUID=8487960720141044728L;
		Upload uploader;
		ListSelect importTypes;
		
		public CanvasImport(){
			uploader=new Upload("Upload",this);
			
			uploader.addListener((Upload.SucceededListener)this);
			uploader.addListener((Upload.FailedListener)this);
			
			
			
			List<String> importTypeList=theCanvas.getImportFormats();
			
			importTypes=new ListSelect("Format", importTypeList);
			importTypes.setNullSelectionAllowed(false);
			importTypes.setValue(importTypeList.get(0));
			
			getContent().addComponent(importTypes);
			getContent().addComponent(uploader);
		}
		
		File file;
		
		@Override
		public OutputStream receiveUpload(String filename, String MIMEType) {
			FileOutputStream fos = null;
	        try {
	        	file = File.createTempFile("import",".seq");
	        	
	        	addLocalResource(file); 
	        	
	            fos = new FileOutputStream(file);
	        } catch (final java.io.FileNotFoundException e) {
	        	// TODO Auto-generated catch block
				e.printStackTrace();
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        return fos;
		}

		@Override
		public void uploadFailed(FailedEvent event) {
			showMessage("Upload failed","100px","100px");
		}

		@Override
		public void uploadSucceeded(SucceededEvent event) {
			String format=theCanvas.getImportFormatShortFormat((String)importTypes.getValue());
			try{
				theCanvas.theDoc.importFrom(file.getPath(), format);
			}catch(Exception ex){
				showMessage("Wrong format or corrupt file!","350px","150px");
			}
			
			if(LogUtils.hasLastError()){
				showMessage("Wrong format or corrupt file!","350px","150px");
			}
			
			LogUtils.clearLastError();
			
			try{
				file.delete();
			}catch(Exception ex){
				//do something
			}
			
			removeLocalResource(file);
		}
	}
	
	public void showMessage(String message,String width, String height){
		showMessage(message,width,height,"");
	}
	
	public void showMessage(String message,String width, String height,String caption){
		Window window=new Window();
		window.setCaption(caption);
		window.setWidth(width);
		window.setHeight(height);
		
		window.center();
		
		Panel panel=new Panel();
		
		panel.getContent().addComponent(new Label(message));
		
		window.getContent().addComponent(panel);
		
		getWindow().addWindow(window);
	}
	
	public void addLocalResource(File file){
		for(LocalResourceWatcher watcher:localResourceWatchers){
			watcher.addLocalResource(file);
		}
	}
	
	public void removeLocalResource(File file){
		for(LocalResourceWatcher watcher:localResourceWatchers){
			watcher.removeLocalResource(file);
		}
	}
	
	public void appendExportMenu(MenuBar.MenuItem parent){
		MenuBar.MenuItem exportMenu=parent.addItem("Export", null);
		
		Command command=new Command(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-3300979660830126504L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				if(theCanvas.theDoc.getStructures().size()==0){
					return;
				}
				
				String format=theCanvas.getInternalFormat(selectedItem.getText());
				try {
					File file = File.createTempFile("seq", "."+format);
					
					addLocalResource(file);
					
					theCanvas.theDoc.exportTo(file.getPath(), format);
					
					String mimeType="text/plain";
					
					if(format.equals("glycoct_xml") || format.equals("glyde")){
						mimeType="text/xml";
					}
					
					final String finalMimeType=mimeType;
					
					getWindow().open(new FileResource(file, getApplication()){
						/**
						 * 
						 */
						private static final long serialVersionUID=6816460214678812683L;

						@Override
						public DownloadStream getStream(){
							try{
								final DownloadStream ds=new DownloadStream(new FileInputStream(getSourceFile()),finalMimeType,getFilename());
								ds.setParameter("Content-Disposition", "attachment; filename=\"" +getFilename() + "\"");
								ds.setCacheTime(getCacheTime());
								return ds;
							}catch(Exception ex){
								
							}
							
							return null;
						}
					});
					
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		};
		
		for(String format:theCanvas.getSequenceExportFormats()){
			exportMenu.addItem(format, command);
		}
		
		Command onImageExport=new Command(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-3304865968113708422L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				if(theCanvas.theDoc.getStructures().size()==0){
					return;
				}
				
				String format=theCanvas.getImageExportShortFormat(selectedItem.getText());
				try {
					File file = File.createTempFile("seq", "."+format);
					
					for(LocalResourceWatcher watcher:localResourceWatchers){
						watcher.addLocalResource(file);
					}
					
					GlycanRendererAWT renderer=new GlycanRendererAWT((AbstractGlycanRenderer)theCanvas.theGlycanRenderer);
					
					SVGUtils.export(renderer,file.getPath(),theCanvas.theDoc.getStructures(),theCanvas.theWorkspace.getGraphicOptions().SHOW_MASSES_CANVAS,theCanvas.theWorkspace.getGraphicOptions().SHOW_REDEND_CANVAS,format);
											
					String mimeType="image/"+format;
					
					if(format.equals("pdf")){
						mimeType="application/pdf";
					}else if(format.equals("ps")){
						mimeType="application/postscript";
					}else if(format.equals("svg")){
						mimeType=mimeType.concat("+xml");
					}
					
					final String finalMimeType=mimeType;
					
					getWindow().open(new FileResource(file, getApplication()){
						/**
						 * 
						 */
						private static final long serialVersionUID=7843576185418877104L;

						@Override
						public DownloadStream getStream(){
							try{
								final DownloadStream ds=new DownloadStream(new FileInputStream(getSourceFile()),finalMimeType,getFilename());
								ds.setParameter("Content-Disposition", "attachment; filename=\"" +getFilename() + "\"");
								ds.setCacheTime(getCacheTime());
								return ds;
							}catch(Exception ex){
								
							}
							
							return null;
						}
					});
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		
		MenuBar.MenuItem exportToImageMenu=parent.addItem("Image export", null);
		for(String format:theCanvas.getImageExportFormats()){
			exportToImageMenu.addItem(format, onImageExport);
		}
	}
	
	public void createAddResidueMenu(MenuBar.MenuItem parent) {
		MenuBar.MenuItem structureMenu=parent.addItem("Add residue", null);

		for (String superclass : ResidueDictionary.getSuperclasses()) {
			MenuBar.MenuItem superClassMenu=structureMenu.addItem(superclass,null);
			for (ResidueType t : ResidueDictionary.getResidues(superclass)) {
				if (t.canHaveParent()){
					superClassMenu.addItem(t.getName(), new Command(){
						/**
						 * 
						 */
						private static final long serialVersionUID=4750928193466060500L;

						@Override
						public void menuSelected(MenuItem selectedItem) {
							theCanvas.addResidueByNameToSelected(selectedItem.getText());
						}		
					});
				}		
			}
			
			if(superClassMenu.getChildren()==null){
				structureMenu.removeChild(superClassMenu);
			}
			
		}
	}
	
	public void createInsertResidueMenu(MenuBar.MenuItem parent){
		MenuBar.MenuItem structureMenu=parent.addItem("Insert residue", null);

		for (String superclass : ResidueDictionary.getSuperclasses()) {
			MenuBar.MenuItem superClassMenu=structureMenu.addItem(superclass,null);
			for (ResidueType t : ResidueDictionary.getResidues(superclass)) {
				if (t.canHaveParent() && t.canHaveChildren()
						&& t.getMaxLinkages() >= 2){
					superClassMenu.addItem(t.getName(), new Command(){

						/**
						 * 
						 */
						private static final long serialVersionUID=2685831199169131556L;

						@Override
						public void menuSelected(MenuItem selectedItem) {
							theCanvas.insertResidueByNameBeforeSelected(selectedItem.getText());
						}
						
					});
					
				}	
			}
			if(superClassMenu.getChildren()==null){
				structureMenu.removeChild(superClassMenu);
			}
		}
		structureMenu.setEnabled(false);
		menuItemsWithResidueSelectionDependency.add(structureMenu);
	}
	
	private void createChangeResidueMenu(MenuBar.MenuItem parent) {
		MenuBar.MenuItem structureMenu=parent.addItem("Change residue", null);

		for (String superclass : ResidueDictionary.getSuperclasses()) {
			MenuBar.MenuItem superClassMenu=structureMenu.addItem(superclass,null);
			for (ResidueType t : ResidueDictionary.getResidues(superclass)){
				superClassMenu.addItem(t.getName(), new Command(){

					/**
					 * 
					 */
					private static final long serialVersionUID=-7886271503255704127L;

					@Override
					public void menuSelected(MenuItem selectedItem) {
						theCanvas.changeSelectedToResidueByName(selectedItem.getText());
					}
				});
			}
			if(superClassMenu.getChildren()==null){
				structureMenu.removeChild(superClassMenu);
			}
		}
		
		structureMenu.setEnabled(false);
		menuItemsWithResidueSelectionDependency.add(structureMenu);
	}
	
	public void appendNotationMenu(MenuBar.MenuItem parent) {
		final HashMap<String,String> notationIndex=new HashMap<String,String>();
		
		Command notationChangeCommand=new Command(){
			/**
			 * 
			 */
			private static final long serialVersionUID=5081687058270283137L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				String notation=notationIndex.get(selectedItem.getText());
				theCanvas.setNotation(notation);
			}
		};

		parent.addItem("CFG notation", notationChangeCommand);
		notationIndex.put("CFG notation", GraphicOptions.NOTATION_CFG);
		
		parent.addItem("CFG black and white notation", notationChangeCommand);
		notationIndex.put("CFG black and white notation", GraphicOptions.NOTATION_CFGBW);
		
		parent.addItem("CFG with linkage placement notation", notationChangeCommand);
		notationIndex.put("CFG with linkage placement notation", GraphicOptions.NOTATION_CFGLINK);

		parent.addItem("UOXF notation", notationChangeCommand);
		notationIndex.put("UOXF notation", GraphicOptions.NOTATION_UOXF);
		
		parent.addItem("UOXFCOL notation", notationChangeCommand);
		notationIndex.put("UOXFCOL notation", GraphicOptions.NOTATION_UOXFCOL);

		parent.addItem("Text only notation", notationChangeCommand);
		notationIndex.put("Text only notation", GraphicOptions.NOTATION_TEXT);
		
		parent.addItem("Show Masses", new ThemeResource("icons/uncheckedbox.png"),new Command(){
			/**
			 * 
			 */
			private static final long serialVersionUID=6140157670134115820L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				//selectedItem.setIcon(arg0);
				
				boolean showMasses=theCanvas.theWorkspace.getGraphicOptions().SHOW_MASSES_CANVAS;
				
				if(showMasses){
					theCanvas.theWorkspace.getGraphicOptions().SHOW_MASSES_CANVAS=false;
					selectedItem.setIcon(new ThemeResource("icons/uncheckedbox.png"));
				}else{
					theCanvas.theWorkspace.getGraphicOptions().SHOW_MASSES_CANVAS=true;
					selectedItem.setIcon(new ThemeResource("icons/checkbox.png"));
				}
				
				theCanvas.documentUpdated();
			}
		});
		
		parent.addItem("Show reducing end symbol",new ThemeResource("icons/uncheckedbox.png"), new Command(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-5209359926737326181L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				//selectedItem.setIcon(arg0);
				
				boolean showRedEnd=theCanvas.theWorkspace.getGraphicOptions().SHOW_REDEND_CANVAS;
				
				if(showRedEnd){
					theCanvas.theWorkspace.getGraphicOptions().SHOW_REDEND_CANVAS=false;
					selectedItem.setIcon(new ThemeResource("icons/uncheckedbox.png"));
				}else{
					theCanvas.theWorkspace.getGraphicOptions().SHOW_REDEND_CANVAS=true;
					selectedItem.setIcon(new ThemeResource("icons/checkbox.png"));
				}
				
				theCanvas.documentUpdated();
			}
		});
		
		
		final Window massOptionsDialog=new Window(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-5094399884130705221L;

			public void close(){
				setVisible(false);
			}
		};
		
		massOptionsDialog.setCaption("Mass options");
		
		MassOptionsDialog dialog=new MassOptionsDialog(theCanvas.theDoc.getStructures(), theCanvas.theWorkspace.getDefaultMassOptions());
		
		dialog.addMassOptionListener(this);
		
		massOptionsDialog.addComponent(dialog);
		
		((VerticalLayout) massOptionsDialog.getContent()).setComponentAlignment(dialog, Alignment.MIDDLE_CENTER);
		
		massOptionsDialog.setVisible(false);
		massOptionsDialog.setWidth("600px");
		massOptionsDialog.setHeight("400px");
		
		
		
		
		massOptionsDialog.center();
		
		parent.addItem("Mass options",new Command(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-589321392382766804L;

			@Override
			public void menuSelected(MenuItem selectedItem) {
				getWindow().addWindow(massOptionsDialog);
				massOptionsDialog.setVisible(true);	
			}
		});
	}
	
	
	
	public void enableResidueToolBarMode(){
		theCanvas.theGlycanRenderer.setRenderMode(GlycanRendererMode.TOOLBAR);
		appendResidueToolBar();
	}
	
	public void clearAll(){
		clear(); //clear remote canvas
		theCanvas.clear(); //clear residue/linkage selection state and the glycan document of all structures
	}
	
	public void appendResidueToolBar(){
		theCanvas.setDocumentChangedEventFiring(false);
		
		HashSet<String> selectedResiduesByName=new HashSet<String>();
		for(Residue res:theCanvas.selectedResidues){
			selectedResiduesByName.add(res.getTypeName());
		}
	
		clearAll();
		
		HashSet<String> drawn=new HashSet<String>();
		
		for(Iterator<ResidueType> i=ResidueDictionary.directResidues().iterator();i.hasNext();) {
			ResidueType t = i.next();
				
			if(t.canHaveParent()){
				theCanvas.addResidueByNameToSelected(t.getName());
				drawn.add(t.getName());
			}
		}
		
		int numberOfResiduesInHistory=theCanvas.residueTypeHistoryList.size();
		if(theCanvas.residueTypeHistoryList.size()>0){
			for(int i=0;i<numberOfResiduesInHistory;i++){
				String name=theCanvas.residueTypeHistoryList.get(i);
				if(!drawn.contains(name)){
					theCanvas.addResidueByNameToSelected(name);
				}
			}
		}
		
		theCanvas.selectAllResiduesByName(selectedResiduesByName);
		
		theCanvas.documentUpdated();
		
		theCanvas.setDocumentChangedEventFiring(true);
		
		requestRepaint();
	}
	
	public void setResidueHistoryList(Vector<String> residueHistoryList){
		theCanvas.residueTypeHistoryList=residueHistoryList;
	}
	
	/**
	 * Set the properties of the residue with the focus using the values in the
	 * properties toolbar
	 * 
	 * @see #getToolBarProperties
	 */
	public void onSetProperties() {
		Residue current = theCanvas.getCurrentResidue();
		if (current != null) {
			current.setAnomericState(getSelectedValueChar(field_anomeric_state));
			current.setAnomericCarbon(getSelectedValueChar(field_anomeric_carbon));
			current.setChirality(getSelectedValueChar(field_chirality));
			current.setRingSize(getSelectedValueChar(field_ring_size));

			Linkage parent_linkage = current.getParentLinkage();
			if (parent_linkage != null) {
				char[] sel_linkage_positions = getSelectedPositions(field_linkage_position);
				if((Boolean)field_second_bond.getValue()){
					char[] sel_second_parent_positions = getSelectedPositions(field_second_parent_position);
					char sel_second_child_position = getSelectedValueChar(field_second_child_position);
					parent_linkage.setLinkagePositions(sel_linkage_positions,
							sel_second_parent_positions,
							sel_second_child_position);
				}else{
					parent_linkage.setLinkagePositions(sel_linkage_positions);
				}
			}

			theCanvas.documentUpdated();
		}
	}
	
	/**
	 * Change the properties of the residue with the focus. Display a
	 * {@link ResiduePropertiesDialog} or a {@link RepetitionPropertiesDialog}
	 * depending of the type of the residue.
	 */
	public void onProperties() {
		Residue current = theCanvas.getCurrentResidue();
		if (current == null)
			return;
		if (current.getParent() != null
				&& (!current.isSpecial() || current.isCleavage())) {
			//new ResiduePropertiesDialog(theCanvas.theParent, current,
				//	theCanvas.getLinkedResidues(), theCanvas.theDoc).setVisible(true);
			theCanvas.setSelection(current);
			theCanvas.documentUpdated();
		} else if (current.isStartRepetition()) {
			//new ResiduePropertiesDialog(theCanvas.theParent, current, theCanvas.theDoc)
				//	.setVisible(true);
			theCanvas.setSelection(current);
			theCanvas.documentUpdated();
		} else if (current.isEndRepetition()) {
			//new RepetitionPropertiesDialog(theCanvas.theParent, current, theCanvas.theDoc)
				//	.setVisible(true);
			theCanvas.setSelection(current);
			theCanvas.documentUpdated();
		}
	}
	
	private char[] getSelectedPositions(AbstractSelect field) {
		@SuppressWarnings("rawtypes")
		Set sel = (Set) field.getValue();
		
		if(sel.size() == 0){
			return new char[] { '?' };
		}else{
			char[] ret = new char[sel.size()];
			int i=0;
			for(Object obj:sel){
				ret[i] = ((String) obj).charAt(0);
				i++;
			}
			
			return ret;
		}
	}
	
	private char getSelectedValueChar(AbstractSelect field) {
		if (field.getValue() == null)
			return '?';
		return ((String) field.getValue()).charAt(0);
	}
	
	private void updateLinkageToolBar() {
		if(linkagePanel==null){
			return;
		}else{
			final Residue current = theCanvas.getCurrentResidue();
			
			if(field_anomeric_state!=null){
				field_anomeric_state.removeListener(defaultListener);
				field_anomeric_state.removeListener(defaultListener);
				field_anomeric_carbon.removeListener(defaultListener);
				field_linkage_position.removeListener(defaultListener);
				field_chirality.removeListener(defaultListener);
				field_ring_size.removeListener(defaultListener);
				field_second_child_position.removeListener(defaultListener);
				field_second_parent_position.removeListener(defaultListener);
			}
			
			linkagePanel.removeAllComponents();
			linkage_two_panel = new PopupButton("2nd Linkage");
			popupLayout2 = new HorizontalLayout();
			popupLayout2.addStyleName("2nd_linkage_panel");
			linkage_two_panel.setComponent(popupLayout2);
			
			field_anomeric_state = new OptionGroup("Anomeric state",Arrays.asList(new String[] { "?","a", "b" }));
			field_anomeric_state.setStyleName("linkage_component_select");
			field_anomeric_state.setEnabled(false);
			
			field_anomeric_carbon = new OptionGroup("Anomeric carbon",Arrays.asList(new String[] { "?","1", "2", "3" }));
			field_anomeric_carbon.setEnabled(false);
					
			field_linkage_position = new OptionGroup("Linkage position",Arrays.asList(new String[] { "?", "1", "2", "3", "4", "5", "6", "7", "8", "9" }));
			field_linkage_position.setMultiSelect(true);
			field_linkage_position.setEnabled(false);
			
			field_chirality = new OptionGroup("Chirality",Arrays.asList(new String[] {  "?","D","L" }));
			field_chirality.setMultiSelect(false);
			field_chirality.setEnabled(false);
			
			field_ring_size = new OptionGroup("Ring",Arrays.asList(new String[] {  "?", "p","f", "o" }));
			field_ring_size.setMultiSelect(false);
			field_ring_size.setEnabled(false);
			
			field_second_bond=new CheckBox("2nd bond");
			field_second_bond.setEnabled(false);
			
			
			field_second_bond.setValue(false);
			field_second_bond.setImmediate(true); //forces a value change event to be fired immediately
			
			field_second_child_position = new OptionGroup("Linkage Position",Arrays.asList(new String[] {  "?", "1", "2", "3" }));
			field_second_child_position.setMultiSelect(false);
			field_second_child_position.setEnabled(false);
			
			field_second_parent_position = new OptionGroup("Linkage Position",Arrays.asList(new String[] {  "?", "1", "2", "3", "4", "5", "6", "7","8", "9" }));
			field_second_parent_position.setMultiSelect(true);
			field_second_parent_position.setEnabled(false);
		
			if(current != null && (!current.isSpecial() || current.isCleavage())){
				linkagePanel.setVisible(true);
				
				Linkage parent_link = current.getParentLinkage();
				
				if(parent_link != null){
					field_linkage_position.removeAllItems();
					List<String> positions=theCanvas.createPositions(parent_link.getParentResidue());
					for(String position:positions){
						field_linkage_position.addItem(position);
					}

					field_second_parent_position.removeAllItems();
					for(String position:positions){
						field_second_parent_position.addItem(position);
					}
				}

				boolean can_have_parent_linkage = (parent_link != null && parent_link.getParentResidue() != null && 
						(parent_link.getParentResidue().isSaccharide() || parent_link.getParentResidue().isBracket() || 
								parent_link.getParentResidue().isRepetition() || parent_link.getParentResidue().isRingFragment()));

				field_linkage_position.setEnabled(can_have_parent_linkage);
				field_anomeric_state.setEnabled(current.isSaccharide());
				field_anomeric_carbon.setEnabled(current.isSaccharide());
				field_chirality.setEnabled(current.isSaccharide());
				field_ring_size.setEnabled(current.isSaccharide());
				field_second_bond.setEnabled(can_have_parent_linkage);
				
				linkage_two_panel.setEnabled(can_have_parent_linkage && parent_link.hasMultipleBonds());
				
				field_second_parent_position.setEnabled(can_have_parent_linkage && parent_link.hasMultipleBonds());
				field_second_child_position.setEnabled(can_have_parent_linkage && parent_link.hasMultipleBonds());

				if (parent_link != null){
					field_linkage_position.setValue(Arrays.asList(theCanvas.toStrings(parent_link.glycosidicBond().getParentPositions())));
				}else{
					//field_linkage_position.setNullSelectionItemId(new Integer(-1));
					field_linkage_position.setValue(null);
				}

				field_anomeric_state.setValue(""
						+ current.getAnomericState());
				field_anomeric_carbon.setValue(""
						+ current.getAnomericCarbon());
				field_chirality.setValue("" + current.getChirality());
				field_ring_size.setValue("" + current.getRingSize());
				if (parent_link != null) {
					field_second_bond.setValue(parent_link.hasMultipleBonds());
					field_second_parent_position.setValue(Arrays.asList(theCanvas.toStrings(parent_link.getBonds()
							.get(0).getParentPositions())));
					field_second_child_position.setValue(""+ parent_link.getBonds().get(0).getChildPosition());
				}else{
					field_second_parent_position.setValue(null);
					field_second_child_position.setValue("?");
				}
						
				forceLinkagePopRepaint();
			}else{
				linkagePanel.setVisible(false);
				
				field_linkage_position.setEnabled(false);
				field_anomeric_state.setEnabled(false);
				field_anomeric_carbon.setEnabled(false);
				field_chirality.setEnabled(false);
				field_ring_size.setEnabled(false);
				field_second_bond.setEnabled(false);
				field_second_parent_position.setEnabled(false);
				field_second_child_position.setEnabled(false);

				field_linkage_position.setValue(null);
				field_anomeric_state.setValue("?");
				field_anomeric_carbon.setValue("");
				field_chirality.setValue("?");
				field_ring_size.setValue("?");
				field_second_parent_position.setValue(null);
				field_second_child_position.setValue("?");
				
				forceLinkagePopRepaint();
			}
			
			
		}
	}
	
	public void registerLinkageListeners(){
		
		defaultListener = new Property.ValueChangeListener() {
			/**
			 * 
			 */
			private static final long serialVersionUID=375264676842546361L;

			@Override
			public void valueChange(ValueChangeEvent event) {
				onSetProperties();
				System.err.println("Value change listener");
			}
		};
		
		field_anomeric_state.addListener(defaultListener);
		field_anomeric_carbon.addListener(defaultListener);
		field_linkage_position.addListener(defaultListener);
		field_chirality.addListener(defaultListener);
		field_ring_size.addListener(defaultListener);
		field_second_child_position.addListener(defaultListener);
		field_second_parent_position.addListener(defaultListener);
		
		field_second_bond.addListener(new Property.ValueChangeListener() {
			/**
			 * 
			 */
			private static final long serialVersionUID=1577488601988220206L;

			@Override
			public void valueChange(ValueChangeEvent event) {
				boolean checked=(Boolean) event.getProperty().getValue();
				
				onSetProperties();
				
				Residue current=theCanvas.getCurrentResidue();
				
				System.err.println("Field box changed!");
				
				if(checked){
					Linkage parent_link=current.getParentLinkage();
					boolean can_have_parent_linkage = (parent_link != null && parent_link.getParentResidue() != null && 
							(parent_link.getParentResidue().isSaccharide() || parent_link.getParentResidue().isBracket() || 
									parent_link.getParentResidue().isRepetition() || parent_link.getParentResidue().isRingFragment()));
					
					field_second_parent_position.setEnabled(can_have_parent_linkage && parent_link.hasMultipleBonds());
					field_second_child_position.setEnabled(can_have_parent_linkage && parent_link.hasMultipleBonds());
					
					System.err.println("Enabling second panel: ");
					
					linkage_two_panel.setEnabled(true);
				}else{
					linkage_two_panel.setEnabled(false);
				}
			}
		});
	}
	
	
	public void forceLinkagePopRepaint(){
		registerLinkageListeners();
		
		HorizontalLayout toolBar=new HorizontalLayout();
		
		linkage_one_panel = new PopupButton("1st Linkage");
		if(field_linkage_position.isEnabled() || field_anomeric_carbon.isEnabled() || 
					field_anomeric_state.isEnabled() || field_chirality.isEnabled() || field_ring_size.isEnabled()){
			linkage_one_panel.setEnabled(true);
		}else{
			linkage_one_panel.setEnabled(false);
		}
		
		
		HorizontalLayout popupLayout = new HorizontalLayout();
		popupLayout.addStyleName("1st_linkage_panel");
		
		popupLayout.addComponent(field_anomeric_state);
		popupLayout.addComponent(field_anomeric_carbon);
		popupLayout.addComponent(field_linkage_position);
		popupLayout.addComponent(field_chirality);
		popupLayout.addComponent(field_ring_size);
		
		linkage_one_panel.setComponent(popupLayout);
		
		toolBar.addComponent(linkage_one_panel);
		
		popupLayout2.addComponent(field_second_child_position);
		popupLayout2.addComponent(field_second_parent_position);
		
		toolBar.addComponent(field_second_bond);
		
		toolBar.addComponent(linkage_two_panel);
		
		linkagePanel.removeAllComponents();
		linkagePanel.setContent(toolBar);
		
		linkagePanel.requestRepaintAll();
	}
	
	Panel linkagePanel;

	private HorizontalLayout popupLayout2;

	private Property.ValueChangeListener defaultListener;
	
	public void appendLinkageToolBar(Panel theLinkageToolBarPanel){
		linkagePanel=theLinkageToolBarPanel;
		
		updateLinkageToolBar();	
	}
	
	public void appendGeneralToolBar(Panel theToolBarPanel){
		HorizontalLayout toolBar=new HorizontalLayout();
		
		toolBar.setStyleName("toolbar");
		
		NativeButton deleteButton=new NativeButton("Delete");
		deleteButton.setIcon(new ThemeResource("icons/deleteNew.png"));
		deleteButton.addListener(new ClickListener(){
			/**
			 * 
			 */
			private static final long serialVersionUID=1289257412952359727L;

			@Override
			public void buttonClick(ClickEvent event) {
				theCanvas.delete();
			}
		});
		deleteButton.setEnabled(false);
		
		NativeButton copyButton=new NativeButton("Copy");
		final NativeButton pasteButton=new NativeButton("Paste");
		copyButton.setIcon(new ThemeResource("icons/editcopy.png"));
		copyButton.addListener(new ClickListener(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-1740735587078805580L;

			@Override
			public void buttonClick(ClickEvent event) {
				System.err.println("Copy");
				theCanvas.copy();
				pasteButton.setEnabled(true);
			}
		});
		copyButton.setEnabled(false);
		
		pasteButton.setIcon(new ThemeResource("icons/editpaste.png"));
		pasteButton.addListener(new ClickListener(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-8732259244009686729L;

			@Override
			public void buttonClick(ClickEvent event) {
				theCanvas.paste();
				pasteButton.setEnabled(false);
			}
		});
		pasteButton.setEnabled(false);
		
		final NativeButton bracketButton=new NativeButton("Bracket");
		bracketButton.setIcon(new ThemeResource("icons/bracket.png"));
		bracketButton.addListener(new ClickListener(){
			/**
			 * 
			 */
			private static final long serialVersionUID=5201094306113759901L;

			@Override
			public void buttonClick(ClickEvent event) {
				theCanvas.addBracket();
			}
		});
		bracketButton.setEnabled(false);
		
		final NativeButton repeatButton=new NativeButton("Repeat");
		repeatButton.setIcon(new ThemeResource("icons/repeat.png"));
		repeatButton.addListener(new ClickListener(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-23302591439643695L;

			@Override
			public void buttonClick(ClickEvent event) {
				try {
					theCanvas.addRepeat();
				} catch (Exception ex) {
					showMessage(ex.getMessage(), "400px","100px","Error creating repeat");
				}
			}
		});
	    repeatButton.setEnabled(false);
		
	    componentsWithResidueSelectionDependency.add(repeatButton);
		componentsWithResidueSelectionDependency.add(bracketButton);
		componentsWithResidueSelectionDependency.add(deleteButton);
		componentsWithResidueSelectionDependency.add(copyButton);
		
		final NativeButton orientationButton=new NativeButton("Orientation");
		
		orientationButton.setIcon(new ThemeResource("icons/"+theCanvas.getOrientationIcon()));
		orientationButton.addListener(new ClickListener(){
			/**
			 * 
			 */
			private static final long serialVersionUID=6621021858668446143L;

			@Override
			public void buttonClick(ClickEvent event) {
				theCanvas.changeOrientation();
				orientationButton.setIcon(new ThemeResource("icons/"+theCanvas.getOrientationIcon()));
			}
		});
		
		final NativeButton selectAllButton=new NativeButton("Select all");
		
		selectAllButton.setIcon(new ThemeResource("icons/selectall.png"));
		selectAllButton.addListener(new ClickListener(){
			/**
			 * 
			 */
			private static final long serialVersionUID=-5848923636575805154L;

			@Override
			public void buttonClick(ClickEvent event) {
				theCanvas.selectAll();
				theCanvas.documentUpdated();
			}
		});
		
		final NativeButton deSelectAllButton=new NativeButton("Select none");
		
		deSelectAllButton.setIcon(new ThemeResource("icons/deselect.png"));
		deSelectAllButton.addListener(new ClickListener(){
			/**
			 * 
			 */
			private static final long serialVersionUID=8339468775345706027L;

			@Override
			public void buttonClick(ClickEvent event) {
				theCanvas.resetSelection();
				theCanvas.documentUpdated();
			}
		});
		
		toolBar.addComponent(deleteButton);
		toolBar.addComponent(copyButton);
		toolBar.addComponent(pasteButton);
		toolBar.addComponent(orientationButton);
		toolBar.addComponent(selectAllButton);
		toolBar.addComponent(deSelectAllButton);
		toolBar.addComponent(bracketButton);
		toolBar.addComponent(repeatButton);
		
		theToolBarPanel.setContent(toolBar);
	}
	
	public void updateActions(){
		if(theCanvas.hasSelectedResidues()){
			for(Component component:componentsWithResidueSelectionDependency){
				component.setEnabled(true);
			}
			
			for(MenuBar.MenuItem menuItem:menuItemsWithResidueSelectionDependency){
				menuItem.setEnabled(true);
			}
			
		}else{
			for(MenuBar.MenuItem menuItem:menuItemsWithResidueSelectionDependency){
				menuItem.setEnabled(false);
			}
			
			for(Component component:componentsWithResidueSelectionDependency){
				component.setEnabled(false);
			}
		}
	}
	
	public void automaticHeightAdjust(boolean automaticallyAdjustHeight){
		this.automaticallyAdjustHeight=automaticallyAdjustHeight;
	}
	
	private void updateCanvasHeight(){
		if(automaticallyAdjustHeight){
			//String width=String.valueOf(getWindow().getHeight());
			//System.err.println("Width: "+width);
			setHeight(theCanvas.getHeight()+100+"px");
			//getParent().getContent().setHeight(theCanvas.getHeight()+"px");
			////theParentPanel.requestRepaint();
		}
	}
	
	@Override
	public void recieveSelectionUpdate(double x, double y, double width,double height,boolean mouseMoved) {
		theCanvas.selectIntersectingRectangles(x, y, width, height, mouseMoved);
	}

	@Override
	public void glycanCanvasUpdated() {
		updateActions();
		updateCanvasHeight();
		requestRepaint();
	}

	@Override
	public void selectionChanged() {
		System.err.println("Updating linkage toolbar");
		updateLinkageToolBar();
	}

	@Override
	public void addLocalResourceWatcher(LocalResourceWatcher watcher) {
		localResourceWatchers.add(watcher);
	}

	@Override
	public void removeLocalResourceWatcher(LocalResourceWatcher watcher) {
		localResourceWatchers.remove(watcher);
	}

	@Override
	public void massOptions(MassOptions massOptions,Collection<Glycan> structures) {
		Collection<Glycan> glycans=theCanvas.getSelectedStructures();
		if(glycans.size()==0){
			glycans=theCanvas.theDoc.getStructures();
		}
		
		theCanvas.theDoc.setMassOptions(glycans, massOptions);
		
		theCanvas.theWorkspace.getDefaultMassOptions().setValues(massOptions);
		
		theCanvas.documentUpdated();
	}
}