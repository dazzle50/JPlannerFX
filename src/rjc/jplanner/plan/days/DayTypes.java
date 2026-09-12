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

package rjc.jplanner.plan.days;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/*************************************************************************************************/
/********************** Plan-owned ordered collection of editable day types **********************/
/*************************************************************************************************/

public final class DayTypes implements Iterable<DayType>
{
  private final List<DayType> m_days = new ArrayList<>();                      // ordered day types owned by the plan
  // the view tracks collection updates but prevents mutation through iterators and sublists.
  private final List<DayType> m_view = Collections.unmodifiableList( m_days ); // read-only view of editable day types

  /******************************************** size *********************************************/
  public int size()
  {
    return m_days.size();
  }

  /********************************************* get *********************************************/
  public DayType get( int index )
  {
    return m_days.get( index );
  }

  /******************************************* indexOf *******************************************/
  public int indexOf( DayType day )
  {
    return m_days.indexOf( day );
  }

  /******************************************* asList ********************************************/
  public List<DayType> asList()
  {
    return m_view;
  }

  /****************************************** iterator *******************************************/
  @Override
  public Iterator<DayType> iterator()
  {
    return m_view.iterator();
  }

  /********************************************* add *********************************************/
  public void add( DayType day )
  {
    insertAll( size(), List.of( day ) );
  }

  /****************************************** insertAll ******************************************/
  public void insertAll( int index, List<DayType> proposed )
  {
    // the position immediately after the last row is a valid insertion point.
    Objects.checkIndex( index, size() + 1 );
    List<DayType> copy = List.copyOf( proposed );

    var names = new HashSet<String>();
    for ( DayType day : m_days )
      names.add( day.getName() );

    // check the entire batch before inserting anything, including conflicts within the batch.
    for ( DayType day : copy )
      if ( !names.add( day.getName() ) )
        throw new IllegalArgumentException( "Duplicate day type or name: " + day.getName() );

    m_days.addAll( index, copy );
  }

  /*************************************** validatedRename ***************************************/
  public String validatedRename( DayType day, String name )
  {
    Objects.requireNonNull( day, "day" );
    if ( !m_days.contains( day ) )
      throw new IllegalArgumentException( "DayType type is not in this collection" );

    String cleaned = DayType.validatedName( name );
    // exclude the target so renaming to its current cleaned name remains a valid no-op.
    for ( DayType other : m_days )
      if ( other != day && other.getName().equals( cleaned ) )
        throw new IllegalArgumentException( "Name not unique: " + cleaned );
    return cleaned;
  }

  /******************************************* rename ********************************************/
  public void rename( DayType day, String name )
  {
    day.updateName( validatedRename( day, name ) );
  }

  /***************************************** removeRange *****************************************/
  public List<DayType> removeRange( int index, int count )
  {
    if ( count < 0 )
      throw new IllegalArgumentException( "Negative count" );
    Objects.checkFromIndexSize( index, count, size() );

    // retain the removed objects for undo, rather than a sublist backed by the changing collection.
    List<DayType> removed = List.copyOf( m_days.subList( index, index + count ) );
    m_days.subList( index, index + count ).clear();
    return removed;
  }

  /******************************************** swap *********************************************/
  public void swap( int first, int second )
  {
    Objects.checkIndex( first, size() );
    Objects.checkIndex( second, size() );
    Collections.swap( m_days, first, second );
  }

  /******************************************** clear ********************************************/
  // low-level reset; callers are responsible for calendar references
  public void clear()
  {
    m_days.clear();
  }

  /***************************************** initialise ******************************************/
  public void initialise()
  {
    // calendar defaults rely on this ordering when resolving their initial day references.
    clear();
    add( new DayType( "Non working", 0, List.of() ) );
    add( new DayType( "Standard work day", 1, List.of( WorkPeriod.ofHours( 9, 13 ), WorkPeriod.ofHours( 14, 18 ) ) ) );
    add( new DayType( "Morning only", .5, List.of( WorkPeriod.ofHours( 9, 13 ) ) ) );
    add( new DayType( "Evening shift", .6, List.of( WorkPeriod.ofHours( 18, 22 ) ) ) );
    add( new DayType( "24H day", 1.5, List.of( WorkPeriod.ofHours( 0, 24 ) ) ) );
  }
}
