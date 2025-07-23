/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

package rjc.jplanner.gui.calendars;

import rjc.jplanner.commands.CommandCalendarSetCycleLength;
import rjc.jplanner.plan.Calendar.FIELD;
import rjc.jplanner.plan.Plan;
import rjc.table.Utils;
import rjc.table.data.TableData;
import rjc.table.view.TableView;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.editor.AbstractCellEditor;
import rjc.table.view.editor.EditorChoose;
import rjc.table.view.editor.EditorDate;
import rjc.table.view.editor.EditorInteger;
import rjc.table.view.editor.EditorText;

/*************************************************************************************************/
/******************* Customised table-view for interacting with plan calendars *******************/
/*************************************************************************************************/

public class CalendarsView extends TableView
{

  /**************************************** constructor ******************************************/
  public CalendarsView( TableData data, String name )
  {
    // construct the table view
    super( data, name );
  }

  /******************************************** reset ********************************************/
  @Override
  public void reset()
  {
    // reset table view to custom settings
    super.reset();
    getColumnsAxis().setHeaderSize( 80 );
    getColumnsAxis().setDefaultSize( 140 );
  }

  /**************************************** getCellDrawer ****************************************/
  @Override
  public CellDrawer getCellDrawer()
  {
    // return new instance of class responsible for drawing the cells on canvas
    return new CalendarsCellDrawer();
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
    int field = cell.dataRow > FIELD.Normal.ordinal() ? FIELD.Normal.ordinal() : cell.dataRow;

    switch ( FIELD.values()[field] )
    {
      case Name:
        return new EditorText();

      case Anchor:
        return new EditorDate();

      case Exceptions:
        Utils.trace( "EDIT EXCEPTIONS - NOT YET IMPLEMENTED" );
        return null;

      case Cycle:
        var editorInteger = new EditorInteger()
        {
          @Override
          protected boolean commit()
          {
            // push new command on undo-stack to update cycle-length
            TableData data = cell.view.getData();
            int dataColumn = cell.dataColumn;
            var command = new CommandCalendarSetCycleLength( data, dataColumn, (int) getValue() );
            return cell.view.getUndoStack().push( command );
          }
        };
        editorInteger.setRange( 1, 99 );
        editorInteger.setStepPage( 1, 1 );
        return editorInteger;

      default:
        // normals - select day-types
        return new EditorChoose( Plan.getDays().getNameArray() );
    }
  }

}
