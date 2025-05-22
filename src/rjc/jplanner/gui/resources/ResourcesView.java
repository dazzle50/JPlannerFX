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

package rjc.jplanner.gui.resources;

import rjc.jplanner.plan.Plan;
import rjc.jplanner.plan.Resource.FIELD;
import rjc.table.data.TableData;
import rjc.table.view.TableView;
import rjc.table.view.cell.CellDrawer;
import rjc.table.view.editor.AbstractCellEditor;
import rjc.table.view.editor.EditorChoose;
import rjc.table.view.editor.EditorDate;
import rjc.table.view.editor.EditorDouble;
import rjc.table.view.editor.EditorText;

/*************************************************************************************************/
/******************* Customised table-view for interacting with plan resources *******************/
/*************************************************************************************************/

public class ResourcesView extends TableView
{

  /**************************************** constructor ******************************************/
  public ResourcesView( TableData data, String name )
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
    getColumnsAxis().setIndexSize( FIELD.Available.ordinal(), 70 );
    getColumnsAxis().setIndexSize( FIELD.Cost.ordinal(), 70 );
    getColumnsAxis().setIndexSize( FIELD.Comment.ordinal(), 250 );
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
    switch ( FIELD.values()[cell.dataColumn] )
    {
      case Initials:
        // allow any non-whitespace characters except square brackets and comma
        var editor = new EditorText();
        editor.setAllowed( "^[^\\s\\[\\],]*$" );
        return editor;

      case Name, Organisation, Group, Role, Alias, Comment:
        return new EditorText();

      case Start, End:
        return new EditorDate();

      case Available:
        return new EditorDouble();

      case Calendar:
        return new EditorChoose( Plan.getCalendars().getNameArray() );

      default:
        return null;
    }
  }

}
