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

package rjc.jplanner.gui.tasks;

import java.util.Arrays;
import java.util.BitSet;

import javafx.scene.Node;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.tasks.Task.FIELD;
import rjc.table.HashSetInt;
import rjc.table.signal.ObservableStatus.Level;
import rjc.table.view.axis.TableAxis;
import rjc.table.view.cursor.Cursors;
import rjc.table.view.cursor.RowReorderCursor;

/*************************************************************************************************/
/******************* Task table input safeguards and selection-based targeting *******************/
/*************************************************************************************************/

final class TasksViewInput
{
  private final TasksView m_view;                             // view whose library handlers are extended
  private final BitSet    m_columnHeaderAreas = new BitSet(); // selection areas created by column-header gestures

  /**************************************** constructor ******************************************/
  TasksViewInput( TasksView view )
  {
    m_view = view;

    // prevent the reorder gesture itself, including the library's view-only fallback
    m_view.cursorProperty().addListener( ( observable, oldCursor, cursor ) ->
    {
      if ( cursor instanceof RowReorderCursor )
        m_view.setCursor( Cursors.ROWS_HOVER );
    } );
    // preserve context selections before the library cursor can clear them, for every menu action
    var mousePressed = m_view.getOnMousePressed();
    m_view.setOnMousePressed( event ->
    {
      int viewRow = m_view.getMouseCell().getRow();
      int viewColumn = m_view.getMouseCell().getColumn();
      if ( event.getButton() == MouseButton.SECONDARY && isContextSelected( viewRow, viewColumn ) )
      {
        m_view.requestFocus();
        event.consume();
        return;
      }
      mousePressed.handle( event );
      if ( event.getButton() == MouseButton.PRIMARY )
      {
        int count = m_view.getSelection().getAreas().size();
        var cursor = m_view.getCursor();
        if ( cursor == Cursors.COLUMNS_REORDER )
          m_columnHeaderAreas.set( 0, count );
        else if ( count > 0 && ( cursor == Cursors.COLUMNS_SELECT || cursor == Cursors.ROWS_SELECT
            || cursor == Cursors.CELLS_SELECT || cursor == Cursors.CORNER_CELL ) )
          m_columnHeaderAreas.set( count - 1, cursor == Cursors.COLUMNS_SELECT );
      }
    } );

    var mouseReleased = m_view.getOnMouseReleased();
    m_view.setOnMouseReleased( event ->
    {
      // context clicks never start a resize/reorder gesture, so its primary-button release must not run
      if ( event.getButton() == MouseButton.SECONDARY )
      {
        event.consume();
        return;
      }
      // column reordering rebuilds its column selection on release; resizing leaves origins unchanged
      boolean columnReorder = m_view.getCursor() == Cursors.COLUMNS_REORDER;
      mouseReleased.handle( event );
      if ( columnReorder )
        m_columnHeaderAreas.set( 0, m_view.getSelection().getAreas().size() );
    } );
    m_view.getSelection().addListener( ( sender, message ) ->
    {
      // clear origin markers when areas are removed; newly added areas default to body selections
      int count = (int) message[0];
      if ( count < m_columnHeaderAreas.length() )
        m_columnHeaderAreas.clear( count, m_columnHeaderAreas.length() );
    } );
    m_view.addEventFilter( KeyEvent.KEY_PRESSED, this::checkKeyPressed );
  }

  /*************************************** checkKeyPressed ***************************************/
  private void checkKeyPressed( KeyEvent event )
  {
    // text input inside an active editor retains the editor's ordinary key handling
    for ( Node node = event.getTarget() instanceof Node target ? target : null; node != null; node = node.getParent() )
      if ( node instanceof TextInputControl )
        return;
    if ( event.isAltDown() )
      return;

    updateSelectionOrigins( event );
    if ( event.getCode() == KeyCode.DELETE )
      blockTitleClearing( event );
  }

  /*********************************** updateSelectionOrigins ***********************************/
  private void updateSelectionOrigins( KeyEvent event )
  {
    // keyboard selection changes create body areas rather than column-header-only selections
    switch ( event.getCode() )
    {
      case A:
        if ( event.isControlDown() )
          m_columnHeaderAreas.clear();
        break;
      case RIGHT, KP_RIGHT, LEFT, KP_LEFT, DOWN, KP_DOWN, UP, KP_UP, PAGE_DOWN, PAGE_UP, HOME, END:
        int count = m_view.getSelection().getAreas().size();
        if ( count > 0 )
          m_columnHeaderAreas.clear( count - 1 );
        break;
      default:
        break;
    }
  }

  /************************************* blockTitleClearing **************************************/
  private void blockTitleClearing( KeyEvent event )
  {
    // reject the entire clear before the library can clear other cells around a protected title
    int titleColumn = m_view.getColumnsAxis().getViewIndex( FIELD.Title.ordinal() );
    if ( titleColumn < 0 || m_view.getColumnsAxis().getPixelSize( titleColumn ) == 0 )
      return;
    for ( int[] area : m_view.getSelection().getAreas() )
      if ( area[0] <= titleColumn && titleColumn <= area[2] )
        for ( int row : m_view.getRowsAxis().getAllVisible( Math.max( 0, area[1] ), area[3] ) )
        {
          int dataRow = m_view.getRowsAxis().getDataIndex( row );
          if ( dataRow > 0 && !( (PlanContext) m_view.getData().getUserData() ).getPlan().getTask( dataRow ).isBlank() )
          {
            event.consume();
            m_view.getStatus().update( Level.ERROR, "Delete task rows instead of clearing their titles" );
            return;
          }
        }
  }

  /************************************** isContextSelected **************************************/
  private boolean isContextSelected( int viewRow, int viewColumn )
  {
    if ( viewRow < TableAxis.HEADER || viewRow >= m_view.getRowsAxis().getCount() || viewColumn < TableAxis.HEADER
        || viewColumn >= m_view.getColumnsAxis().getCount() )
      return false;
    if ( viewColumn == TableAxis.HEADER )
      return m_view.getSelection().hasRowSelection( viewRow );
    if ( viewRow == TableAxis.HEADER )
      return m_view.getSelection().hasColumnSelection( viewColumn );
    return m_view.getSelection().isCellSelected( viewColumn, viewRow );
  }

  /************************************** getIndentDataRows **************************************/
  int[] getIndentDataRows( int viewRow, int viewColumn, boolean keyboardTrigger )
  {
    var rows = m_view.getRowsAxis();
    int count = rows.getCount();
    // only a real mouse context click can target a row outside the current selection
    if ( !keyboardTrigger && !isContextSelected( viewRow, viewColumn ) )
      return viewRow >= 0 && viewRow < count && rows.isVisible( viewRow ) ? new int[] { rows.getDataIndex( viewRow ) }
          : new int[0];

    var areas = m_view.getSelection().getAreas();
    if ( areas.isEmpty() || m_columnHeaderAreas.cardinality() == areas.size() )
      return new int[0];

    // count interval starts and ends so overlapping rectangles need only one sweep of body rows
    int[] coverage = new int[count + 1];
    for ( int[] area : areas )
      if ( area[0] <= area[2] && area[1] <= area[3] )
      {
        coverage[area[1]]++;
        coverage[area[3] + 1]--;
      }
    int[] dataRows = new int[count];
    int selected = 0;
    int covered = 0;
    for ( int row = 0; row < count; row++ )
    {
      covered += coverage[row];
      if ( covered > 0 && rows.isVisible( row ) )
        dataRows[selected++] = rows.getDataIndex( row );
    }
    return Arrays.copyOf( dataRows, selected );
  }

  /************************************ getRowActionViewRows *************************************/
  HashSetInt getRowActionViewRows( int viewRow )
  {
    // row actions require whole-row selection; indentation also accepts selected body cells
    if ( viewRow < m_view.getRowsAxis().getCount() && m_view.getSelection().isRowSelected( viewRow ) )
    {
      var rows = m_view.getSelection().getSelectedRows();
      if ( rows != null )
      {
        // adjoining areas can cover a row without any individual area spanning every column
        if ( rows.isEmpty() )
          rows.add( viewRow );
        return rows;
      }
      rows = new HashSetInt();
      for ( int row = 0; row < m_view.getRowsAxis().getCount(); row++ )
        rows.add( row );
      return rows;
    }
    var rows = new HashSetInt();
    rows.add( viewRow );
    return rows;
  }

}
