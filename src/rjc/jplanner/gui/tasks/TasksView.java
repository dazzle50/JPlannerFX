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
import rjc.table.data.TableData;
import rjc.table.view.TableView;

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

}
