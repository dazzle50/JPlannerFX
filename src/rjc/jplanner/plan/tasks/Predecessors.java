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

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.StringJoiner;

import rjc.jplanner.plan.TimeSpan;
import rjc.jplanner.plan.tasks.Dependency.DependencyType;

/*************************************************************************************************/
/********************** Task predecessors shows dependencies on other tasks **********************/
/*************************************************************************************************/

public class Predecessors
{
  private Dependency[]         m_dependencies;
  private WeakReference<Tasks> m_tasksRef;    // weak reference to tasks for toString()

  /**************************************** constructor ******************************************/
  private Predecessors( Tasks tasks )
  {
    // constructor is private to force use of static parse method, which validates predecessor text and sets up weak reference to tasks
    m_tasksRef = new WeakReference<>( tasks );
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
    var predecessors = new Predecessors( tasks );
    predecessors.m_dependencies = new Dependency[parts.length];

    // duplicate predecessor tasks are not allowed, regardless of dependency type or lead/lag
    var seenTasks = new HashSet<Task>();
    var count = 0;

    for ( var part : parts )
    {
      // empty comma sections are ignored, e.g. "1FS,,2SS"
      if ( part.isEmpty() )
        continue;

      // predecessor starts task number, followed by optional dependency type and optional lead/lag, e.g. "1FS+2d"
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

      // dependency type is optional, case-insensitive and defaults to finish-start
      DependencyType type = DependencyType.FS;
      var cut = part.substring( index );
      for ( var possibleType : DependencyType.values() )
        if ( cut.toUpperCase().startsWith( possibleType.name() ) )
        {
          type = possibleType;
          cut = cut.substring( possibleType.name().length() );
          break;
        }

      // check for incomplete dependency type
      if ( !cut.isEmpty() )
      {
        char first = cut.charAt( 0 );
        if ( first == 'S' || first == 'F' || first == 's' || first == 'f' )
          throw new IllegalArgumentException( "Incomplete dependency type in predecessor: " + part );
      }

      // lead/lag is optional and defaults to zero days, but if present must be a signed decimal number followed by a valid time unit character
      var num = 0.0;
      var unit = TimeSpan.Unit.DAYS;

      // check for lead/lag sign, which must be present if there is any lead/lag text
      if ( !cut.isEmpty() )
      {
        var sign = cut.charAt( 0 );
        if ( sign != '+' && sign != '-' )
          throw new IllegalArgumentException( "Missing lead/lag number sign: " + part );
      }

      // if there is lead/lag text, parse it and check for valid format, e.g. "+2d" or "-0.5w"
      if ( cut.length() > 1 )
      {
        // read the signed decimal number, leaving the final character as the unit
        index = 1;
        while ( index < cut.length() && ( Character.isDigit( cut.charAt( index ) ) || cut.charAt( index ) == '.' ) )
          index++;

        if ( index == 1 )
          throw new IllegalArgumentException( "Invalid lead/lag number in predecessor: " + part );

        // accept "+." and "-." as zero, matching the existing permissive decimal parsing style
        if ( index == 2 && cut.charAt( 1 ) == '.' )
        {
          cut = cut.charAt( 0 ) + "0" + cut.substring( 1 );
          index++;
        }

        if ( index >= cut.length() )
          throw new IllegalArgumentException( "Missing lead/lag units in predecessor: " + part );

        num = Double.parseDouble( cut.substring( 0, index ) );
        unit = TimeSpan.Unit.fromChar( cut.charAt( index ) );
        if ( unit == null || index + 1 != cut.length() )
          throw new IllegalArgumentException( "Invalid lead/lag units in predecessor: " + part );
      }

      // dependency stores lead/lag as hundredths of a unit
      predecessors.m_dependencies[count++] = new Dependency( task, type, (int) Math.round( num * 100 ), unit );
    }

    if ( count == 0 )
      return null;

    // trim skipped empty comma sections from the backing array
    if ( count < predecessors.m_dependencies.length )
      predecessors.m_dependencies = Arrays.copyOf( predecessors.m_dependencies, count );

    return predecessors;
  }

  /************************************* hasCircularReference ************************************/
  public boolean hasCircularReference( Task task )
  {
    // circular reference exists if specified task is a predecessor of itself, directly or indirectly through other predecessors
    if ( m_dependencies == null || task == null )
      return false;

    // check each dependency for circular reference to specified task
    for ( var dep : m_dependencies )
    {
      // if dependency task is specified task, circular reference exists
      if ( dep.task == task )
        return true;

      // if dependency task has predecessors, check them for circular reference to specified task
      if ( dep.task.getValue( Task.FIELD.Predecessors.ordinal() ) instanceof Predecessors preds )
        if ( preds.hasCircularReference( task ) )
          return true;
    }

    return false;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // predecessors string is comma-separated list
    var tasks = m_tasksRef.get();
    var sj = new StringJoiner( ", " );

    if ( m_dependencies != null )
      for ( var pred : m_dependencies )
        sj.add( pred.toString( tasks ) );

    return sj.toString();
  }

}