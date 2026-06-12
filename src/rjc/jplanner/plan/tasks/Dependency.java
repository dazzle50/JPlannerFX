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

package rjc.jplanner.plan.tasks;

/*************************************************************************************************/
/********************** Task predecessor dependency with type and lead/lag ***********************/
/*************************************************************************************************/

import rjc.jplanner.plan.TimeSpan;
import rjc.table.Utils;

public class Dependency
{
  public enum DependencyType
  {
    FS, SS, FF, SF // dependency types: finish-start, start-start, finish-finish, start-finish
  }

  public final Task           task;
  public final DependencyType type;
  public final int            lag100ths; // +ve for lag, -ve for lead, 0 for none, *100 to allow for 2 decimal places
  public final TimeSpan.Unit  lagUnit;

  /**************************************** constructor ******************************************/
  public Dependency( Task task, DependencyType type, int num100, TimeSpan.Unit unit )
  {
    // construct dependency with given task reference, dependency type, lead/lag number and units
    this.task = task;
    this.type = type;
    this.lag100ths = num100;
    this.lagUnit = unit;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // dependency debug string shows task name, dependency type and lead/lag
    return Utils.name( task ) + " " + type + " " + lag100ths + " " + lagUnit;
  }

  public String toString( Tasks tasks )
  {
    // dependency display string shows task index, dependency type (if not FS) and lead/lag (if not 0)
    StringBuilder sb = new StringBuilder();
    sb.append( tasks.indexOf( task ) );
    if ( type != DependencyType.FS )
      sb.append( type );
    if ( lag100ths != 0 )
    {
      long num = Math.abs( (long) lag100ths );
      long frac = num % 100;

      sb.append( lag100ths > 0 ? "+" : "-" ).append( num / 100 );
      if ( frac != 0 )
      {
        sb.append( "." ).append( frac / 10 );
        if ( frac % 10 != 0 )
          sb.append( frac % 10 );
      }
      sb.append( lagUnit.abbreviation() );
    }
    return sb.toString();
  }
}
