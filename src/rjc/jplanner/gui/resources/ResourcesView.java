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

import rjc.jplanner.plan.Resource.FIELD;
import rjc.table.data.TableData;
import rjc.table.view.TableView;

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
    getColumnsAxis().setIndexSize( FIELD.Availability.ordinal(), 70 );
    getColumnsAxis().setIndexSize( FIELD.Cost.ordinal(), 70 );
    getColumnsAxis().setIndexSize( FIELD.Comment.ordinal(), 250 );
  }

}
