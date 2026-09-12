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

package rjc.jplanner.gui.days;

import javafx.scene.input.ContextMenuEvent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import rjc.jplanner.commands.CommandInsertDayTypes;
import rjc.table.HashSetInt;
import rjc.table.view.TableContextMenuBuilder;
import rjc.jplanner.commands.CommandDayTypeSetPeriodCount;

import rjc.jplanner.gui.days.DaysData.FIELD;
import rjc.table.data.TableData;
import rjc.table.view.TableContextMenu;
import rjc.table.view.TableContextMenu.TableMenuItems;
import rjc.table.view.TableView;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.editor.AbstractCellEditor;
import rjc.table.view.editor.EditorDouble;
import rjc.table.view.editor.EditorInteger;
import rjc.table.view.editor.EditorText;
import rjc.table.view.editor.EditorTime;

/*************************************************************************************************/
/******************* Customised table-view for interacting with plan day-types *******************/
/*************************************************************************************************/

public class DaysView extends TableView
{

  /**************************************** constructor ******************************************/
  public DaysView( TableData data, String name )
  {
    // construct the table view
    super( data, name );

    // set table view properties
    TableContextMenu.disable( this, TableMenuItems.COLUMN_HIDE, TableMenuItems.COLUMN_SHOW );
    TableContextMenu.disable( this, TableMenuItems.ROW_SORT, TableMenuItems.ROW_FILTER_TEXT );
  }

  /************************************** createContextMenu **************************************/
  public TableContextMenu createContextMenu()
  {
    // replace both row insertion actions because the generic command recreates objects on redo.
    TableContextMenu.setBuilder( this, new TableContextMenuBuilder()
    {
      private ContextMenu m_menu;       // menu receiving day-type insertion actions
      private int         m_triggerRow; // view row that opened the menu

      /**************************************** buildMenu ****************************************/
      @Override
      public ContextMenu buildMenu( ContextMenu menu, TableView view, int row, int column )
      {
        // the superclass keeps these fields private; retain the context needed by our overrides.
        m_menu = menu;
        m_triggerRow = row;
        return super.buildMenu( menu, view, row, column );
      }

      /************************************** addInsertRow ***************************************/
      @Override
      protected TableContextMenuBuilder addInsertRow()
      {
        return addDayInsert( "Insert", m_triggerRow );
      }

      /************************************** addAppendRow ***************************************/
      @Override
      protected TableContextMenuBuilder addAppendRow()
      {
        return addDayInsert( "Add new day-type", getData().getRowCount() );
      }

      /************************************** addDayInsert ***************************************/
      private TableContextMenuBuilder addDayInsert( String label, int row )
      {
        if ( TableContextMenu.isEnabled( DaysView.this, TableMenuItems.ROW_INSERT ) )
        {
          var item = new MenuItem( label );
          item.setOnAction( event -> insertDayRows( row ) );
          m_menu.getItems().add( item );
        }
        return this;
      }
    } );

    try
    {
      return new TableContextMenu( this );
    }
    finally
    {
      // the static registry must not retain this builder's reference to the view.
      TableContextMenu.removeBuilder( this );
    }
  }

  /**************************************** insertDayRows ****************************************/
  // match the table library's selection policy while preserving inserted identities
  public boolean insertDayRows( int triggerRow )
  {
    HashSetInt rows = new HashSetInt();
    if ( getSelection().isRowSelected( triggerRow ) )
    {
      rows = getSelection().getSelectedRows();
      if ( rows == null )
      {
        // the table library represents an all-rows selection with null rather than a full set.
        rows = new HashSetInt();
        for ( int row = 0; row < getData().getRowCount(); row++ )
          rows.add( row );
      }
    }

    // clicking outside the selected rows inserts only at the clicked position, including append.
    if ( rows.isEmpty() )
      rows.add( triggerRow );

    var command = new CommandInsertDayTypes( this, rows );
    if ( command.isValid() )
      getSelection().clear();
    return getUndoStack().push( command );
  }

  /******************************************** reset ********************************************/
  @Override
  public void reset()
  {
    // reset table view to custom settings
    super.reset();
    getColumnsAxis().setHeaderNominalSize( 35 );
    getColumnsAxis().setDefaultNominalSize( 60 );
    getColumnsAxis().setNominalSize( FIELD.Name.ordinal(), 150 );
  }

  /**************************************** getCellDrawer ****************************************/
  @Override
  public CellDrawer getCellDrawer()
  {
    // return new instance of class responsible for drawing the cells on canvas
    return new DaysCellDrawer();
  }

  /**************************************** getCellEditor ****************************************/
  @Override
  public AbstractCellEditor getCellEditor( CellDrawer cell )
  {
    // if cell is disabled (e.g. textPaint is null) then can't edit
    cell.getValueVisual();
    if ( cell.visual.textPaint == null )
      return null;

    // determine editor appropriate for cell
    int field = cell.dataColumn;
    while ( field > FIELD.End.ordinal() )
      field -= 2;

    switch ( FIELD.values()[field] )
    {
      case Name:
        return new EditorText();

      case Work:
        var editorD = new EditorDouble();
        editorD.setRange( 0.0, 9.99 );
        editorD.setFormat( "0.00", 1, 2 );
        editorD.setStepPage( 0.1, 1.0 );
        return editorD;

      case Periods:
        // save full lists for count changes; a scalar count cannot restore deleted period endpoints.
        var editorI = new EditorInteger()
        {
          /*************************************** commit ****************************************/
          @Override
          protected boolean commit()
          {
            // push new command on undo-stack to update work-periods count
            TableData data = cell.view.getData();
            int dataRow = cell.dataRow;
            var command = new CommandDayTypeSetPeriodCount( (DaysData) data, dataRow, (int) getValue() );
            return cell.view.getUndoStack().push( command );
          }
        };
        editorI.setRange( 0, 8 );
        editorI.setStepPage( 1, 1 );
        return editorI;

      default:
        // work period start & end times - hours & minutes only
        return new EditorTime( true, true, false, false );
    }
  }

  /*************************************** openContextMenu ***************************************/
  @Override
  public void openContextMenu( ContextMenuEvent event )
  {
    // build context menu
    var menu = createContextMenu();

    // show the context menu if it has any items
    if ( !menu.getItems().isEmpty() )
      menu.show( getScene().getWindow(), event.getScreenX(), event.getScreenY() );
  }

}
