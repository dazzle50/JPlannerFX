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
/********************** Task predecessor with dependency type and lead/lag ***********************/
/*************************************************************************************************/

import rjc.jplanner.plan.TimeSpan;

public class Predecessor
{
  public enum Type
  {
    FS, SS, FF, SF // dependency types: finish-start, start-start, finish-finish, start-finish
  }

  public final Task          task;
  public final Type          type;
  public final int           num100; // lead/lag number * 100 (to allow for 2 decimal places)
  public final TimeSpan.Unit unit;

  /**************************************** constructor ******************************************/
  public Predecessor( Task task, Type type, int num100, TimeSpan.Unit unit )
  {
    // construct predecessor with given task reference, dependency type, lead/lag number and units
    this.task = task;
    this.type = type;
    this.num100 = num100;
    this.unit = unit;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return string representation of predecessor using task reference
    return toString( task );
  }

  public String toString( Tasks tasks )
  {
    // return string representation of predecessor using task index reference
    return toString( tasks.indexOf( task ) );
  }

  private String toString( Object taskRef )
  {
    // return string representation of predecessor using given task reference, dependency type and lead/lag
    StringBuilder sb = new StringBuilder();
    sb.append( taskRef );
    if ( type != Type.FS )
      sb.append( type );
    if ( num100 != 0 )
    {
      long num = Math.abs( (long) num100 );
      long frac = num % 100;

      sb.append( num100 > 0 ? "+" : "-" ).append( num / 100 );
      if ( frac != 0 )
      {
        sb.append( "." ).append( frac / 10 );
        if ( frac % 10 != 0 )
          sb.append( frac % 10 );
      }
      sb.append( unit.abbreviation() );
    }
    return sb.toString();
  }
}
