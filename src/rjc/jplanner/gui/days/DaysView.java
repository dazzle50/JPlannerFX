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

package rjc.jplanner.gui.days;

import rjc.jplanner.plan.Day;
import rjc.jplanner.plan.Day.FIELD;
import rjc.table.data.TableData;
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
  }

  /******************************************** reset ********************************************/
  @Override
  public void reset()
  {
    // reset table view to custom settings
    super.reset();
    getColumnsAxis().setHeaderSize( 35 );
    getColumnsAxis().setDefaultSize( 60 );
    getColumnsAxis().setIndexSize( Day.FIELD.Name.ordinal(), 150 );
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
        var editorI = new EditorInteger()
        {
          // TODO override commit() to use different undo command
        };
        editorI.setRange( 0, 8 );
        editorI.setStepPage( 1, 1 );
        return editorI;

      default:
        return new EditorTime();
    }
  }

}
