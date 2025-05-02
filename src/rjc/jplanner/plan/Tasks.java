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

package rjc.jplanner.plan;

import java.util.ArrayList;

import rjc.jplanner.Main;

/*************************************************************************************************/
/**************************** Holds the complete list of plan tasks ******************************/
/*************************************************************************************************/

public class Tasks extends ArrayList<Task>
{
  private static final long serialVersionUID = Main.VERSION.hashCode();

  /****************************************** initialise *****************************************/
  public void initialise()
  {
    // initialise list with default tasks (including special task 0 which is overall project summary)
    clear();
    for ( int count = 0; count <= 50; count++ )
      add( new Task() );

    setupTaskZero();
  }

  /**************************************** setupTaskZero ****************************************/
  public void setupTaskZero()
  {
    // setup special task 0
  }

}
