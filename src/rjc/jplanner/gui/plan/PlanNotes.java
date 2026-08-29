/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JPlannerFX                                *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.jplanner.gui.plan;

import java.util.Objects;

import javafx.collections.ListChangeListener;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.PopupWindow;
import javafx.stage.Window;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.Plan;
import rjc.table.undo.IUndoCommand;
import rjc.table.undo.UndoStack;

/*************************************************************************************************/
/************************ Control for displaying & editing of plan notes *************************/
/*************************************************************************************************/

public class PlanNotes extends VBox
{
  private final Plan                 m_plan;
  private final UndoStack            m_undoStack;
  private final TextArea             m_notes         = new TextArea();

  // listener for non-tooltip popups (e.g. menus) when focus within grid
  private ListChangeListener<Window> m_popupListener = change ->
                                                     {
                                                       while ( change.next() )
                                                         for ( Window window : change.getAddedSubList() )
                                                           if ( window instanceof PopupWindow popup
                                                               && !( popup instanceof Tooltip ) && getScene() != null
                                                               && popup.getOwnerWindow() == getScene().getWindow()
                                                               && m_notes.isFocusWithin() )
                                                             updatePlan();
                                                     };

  // undo-command for plan notes edits, which can be merged with consecutive edits
  private final class NotesCommand implements IUndoCommand
  {
    // keep the original notes fixed while consecutive edits replace the latest notes
    private final String m_oldNotes = m_plan.getNotes();
    private String       m_newNotes;

    private NotesCommand( String notes )
    {
      m_newNotes = notes;
      if ( isValid() )
        redo();
    }

    private void merge( String notes )
    {
      // fold a consecutive notes edit into this undo command
      m_newNotes = notes;
      redo();
    }

    @Override
    public void redo()
    {
      // restore the most recently committed notes
      m_plan.setNotes( m_newNotes );
      updateFromPlan();
    }

    @Override
    public void undo()
    {
      // restore the notes from before the first merged edit
      m_plan.setNotes( m_oldNotes );
      updateFromPlan();
    }

    @Override
    public String text()
    {
      return "Plan notes";
    }

    @Override
    public boolean isValid()
    {
      return !Objects.equals( m_oldNotes, m_newNotes );
    }
  }

  /**************************************** constructor ******************************************/
  public PlanNotes( PlanContext context )
  {
    // remember plan and setup notes panel
    m_plan = context.getPlan();
    m_undoStack = context.getUndoStack();

    setPadding( new Insets( 5.0 ) );
    setSpacing( 5.0 );
    setMinWidth( 0.0 );

    // notes area
    m_notes.setWrapText( true );
    getChildren().addAll( new Label( "Notes" ), m_notes );
    m_notes.focusedProperty().addListener( ( property, oldFocus, newFocus ) ->
    {
      if ( !newFocus )
        updatePlan();
    } );

    // notes area should grow to fill all available space
    setVgrow( m_notes, Priority.ALWAYS );
    updateFromPlan();
    Window.getWindows().addListener( new WeakListChangeListener<>( m_popupListener ) );
  }

  /**************************************** updateFromPlan ***************************************/
  public void updateFromPlan()
  {
    // update the gui property widget with text from plan
    m_notes.setText( m_plan.getNotes() );
  }

  /***************************************** updatePlan ******************************************/
  public void updatePlan()
  {
    // if no change in notes, then no need to update plan or undo stack
    if ( m_notes.getText().equals( m_plan.getNotes() ) )
      return;

    // update plan with text from gui property widget via undo command
    if ( !m_undoStack.isClean() && m_undoStack.getUndoCommand() instanceof NotesCommand command )
    {
      // merge consecutive property edits into the latest undo command
      command.merge( m_notes.getText() );
      m_undoStack.commandMerged();
    }
    else
    {
      // new undo command
      m_undoStack.push( new NotesCommand( m_notes.getText() ) );
    }
  }

}
