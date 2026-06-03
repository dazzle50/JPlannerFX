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

import java.util.Arrays;
import java.util.HashSet;
import java.util.StringJoiner;

import rjc.jplanner.plan.TimeSpan;

/*************************************************************************************************/
/********************** Task predecessors shows dependencies on other tasks **********************/
/*************************************************************************************************/

public class Predecessors
{
  private Predecessor[] m_predecessors;

  /**************************************** constructor ******************************************/
  public Predecessors()
  {
    // nothing needs to be done
  }

  /******************************************* parse *********************************************/
  public static Predecessors parse( String text, Tasks tasks )
  {
    if ( tasks == null )
      throw new IllegalArgumentException( "Tasks cannot be null when parsing predecessors" );

    // no predecessor text is represented by null
    if ( text == null || text.isBlank() )
      return null;

    // remove all whitespace before parsing
    var parts = text.replaceAll( "\\s+", "" ).split( "," );
    var predecessors = new Predecessors();
    predecessors.m_predecessors = new Predecessor[parts.length];

    // duplicate predecessor tasks are not allowed, regardless of dependency type or lead/lag
    var seenTasks = new HashSet<Task>();
    var count = 0;

    for ( var part : parts )
    {
      // empty comma sections are ignored, e.g. "1FS,,2SS"
      if ( part.isEmpty() )
        continue;

      // predecessor starts task number, followed by optional dependency type and lead/lag, e.g. "1FS+2d"
      var index = 0;
      while ( index < part.length() && Character.isDigit( part.charAt( index ) ) )
        index++;

      if ( index == 0 )
        throw new IllegalArgumentException( "Missing task number in predecessor: " + part );

      var taskIndex = Integer.parseInt( part.substring( 0, index ) );
      if ( taskIndex < 0 || taskIndex >= tasks.size() )
        throw new IllegalArgumentException( "Invalid task number: " + taskIndex );

      var task = tasks.get( taskIndex );
      if ( task.isBlank() )
        throw new IllegalArgumentException( "Task " + taskIndex + " is blank" );
      if ( !seenTasks.add( task ) )
        throw new IllegalArgumentException( "Duplicate predecessor task: " + taskIndex );

      // dependency type is optional and defaults to finish-start
      var type = Predecessor.Type.FS;
      part = part.substring( index );
      for ( var possibleType : Predecessor.Type.values() )
        if ( part.startsWith( possibleType.name() ) )
        {
          type = possibleType;
          part = part.substring( possibleType.name().length() );
          break;
        }

      // lead/lag is optional and defaults to zero days, but if present must be a signed decimal number followed by a valid time unit character
      var num = 0.0;
      var unit = TimeSpan.Unit.DAYS;

      if ( !part.isEmpty() )
      {
        var sign = part.charAt( 0 );
        if ( sign != '+' && sign != '-' )
          throw new IllegalArgumentException( "Invalid lead/lag number in predecessor: " + part );

        // read the signed decimal number, leaving the final character as the unit
        index = 1;
        while ( index < part.length() && ( Character.isDigit( part.charAt( index ) ) || part.charAt( index ) == '.' ) )
          index++;

        if ( index == 1 )
          throw new IllegalArgumentException( "Invalid lead/lag number in predecessor: " + part );

        // accept "+." and "-." as zero, matching the existing permissive decimal parsing style
        if ( index == 2 && part.charAt( 1 ) == '.' )
          part = part.charAt( 0 ) + "0" + part.substring( 1 );
        if ( index >= part.length() )
          throw new IllegalArgumentException( "Missing lead/lag units in predecessor: " + part );

        num = Double.parseDouble( part.substring( 0, index ) );
        unit = TimeSpan.Unit.fromChar( part.charAt( index ) );
        if ( unit == null || index + 1 != part.length() )
          throw new IllegalArgumentException( "Invalid lead/lag units in predecessor: " + part );
      }

      // predecessor stores lead/lag as hundredths of a unit
      predecessors.m_predecessors[count++] = new Predecessor( task, type, (int) Math.round( num * 100 ), unit );
    }

    if ( count == 0 )
      return null;

    // trim skipped empty comma sections from the backing array
    if ( count < predecessors.m_predecessors.length )
      predecessors.m_predecessors = Arrays.copyOf( predecessors.m_predecessors, count );

    return predecessors;
  }

  /****************************************** toString *******************************************/
  public String toString( Tasks tasks )
  {
    var sj = new StringJoiner( ", " );

    // predecessors string is comma-separated list
    if ( m_predecessors != null )
      for ( var pred : m_predecessors )
        sj.add( pred.toString( tasks ) );

    return sj.toString();
  }
}