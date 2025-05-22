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

package rjc.jplanner.gui.tasks;

import rjc.jplanner.plan.Task.FIELD;
import rjc.jplanner.plan.TaskType;
import rjc.table.data.TableData;
import rjc.table.view.Colours;
import rjc.table.view.TableView;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.editor.AbstractCellEditor;
import rjc.table.view.editor.EditorChoose;
import rjc.table.view.editor.EditorDateTime;
import rjc.table.view.editor.EditorInteger;
import rjc.table.view.editor.EditorText;

/*************************************************************************************************/
/********************* Customised table-view for interacting with plan tasks *********************/
/*************************************************************************************************/

public class TasksView extends TableView
{

  /**************************************** constructor ******************************************/
  public TasksView( TableData data, String name )
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
    getColumnsAxis().setIndexSize( FIELD.Title.ordinal(), 200 );
    getColumnsAxis().setIndexSize( FIELD.Duration.ordinal(), 60 );
    getColumnsAxis().setIndexSize( FIELD.Start.ordinal(), 140 );
    getColumnsAxis().setIndexSize( FIELD.End.ordinal(), 140 );
    getColumnsAxis().setIndexSize( FIELD.Comment.ordinal(), 250 );
  }

  /****************************************** relocate *******************************************/
  @Override
  public void relocate( double x, double y )
  {
    // do nothing when relocate called - to prevent jitter when using split-pane
  }

  /**************************************** getCellEditor ****************************************/
  @Override
  public AbstractCellEditor getCellEditor( CellDrawer cell )
  {
    // if cell is disabled (e.g. textPaint is null) then can't edit
    cell.getValueVisual();
    if ( cell.visual.cellBackground == Colours.CELL_DISABLED_BACKGROUND )
      return null;

    // determine editor appropriate for cell
    switch ( FIELD.values()[cell.dataColumn] )
    {
      case Title, Comment:
        return new EditorText();
      case Start, End:
        return new EditorDateTime();
      case Priority:
        return new EditorInteger();
      case Type:
        return new EditorChoose( TaskType.values() );
      default:
        return null;
    }
  }

}
