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

package rjc.jplanner.gui.gantt;

import javafx.scene.control.ScrollBar;
import rjc.table.view.TableScrollBar;

/*************************************************************************************************/
/*************** Extended version of ScrollBar with special increment & decrement ****************/
/*************************************************************************************************/

class GanttScrollBar extends ScrollBar
{
  /**************************************** constructor ******************************************/
  public GanttScrollBar()
  {
    // construct gantt scroll-bar
    setPrefHeight( TableScrollBar.SIZE );
  }

  /****************************************** increment ******************************************/
  @Override
  public void increment()
  {
    // make gantt end later if scroll-bar at maximum
    super.increment();
  }

  /****************************************** decrement ******************************************/
  @Override
  public void decrement()
  {
    // make gantt start earlier if scroll-bar at minimum
    super.decrement();
  }

}
