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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import rjc.jplanner.plan.resources.Resources;
import rjc.table.Utils;

/*************************************************************************************************/
/************************* Resources assigned to single task within plan *************************/
/*************************************************************************************************/

public class TaskResources
{
  private static final int     MAX_QUANTITY100  = 9999999;                                // 99999.99 units
  private static final Pattern NUMBER_QUANTITY  = Pattern.compile( "\\d+(\\.\\d{1,2})?" );
  private static final Pattern PERCENT_QUANTITY = Pattern.compile( "\\d+" );

  private enum QuantityMode
  {
    ALL_AVAILABLE, EXPLICIT
  }

  private enum DisplayStyle
  {
    NUMBER, PERCENT
  }

  public record Assignment( String tag, boolean allAvailable, double quantity )
  {
  }

  private record ParsedAssignment( String tag, QuantityMode mode, int quantity100, DisplayStyle style )
  {
  }

  private ParsedAssignment[] m_assignments;

  /******************************************* parse *********************************************/
  public static TaskResources parse( String text, Resources resources )
  {
    if ( resources == null )
      throw new IllegalArgumentException( "Resources cannot be null when parsing task resources" );

    // no task resource text is represented by null
    if ( text == null || text.isBlank() )
      return null;

    var assignments = new LinkedHashMap<String, ParsedAssignment>();
    for ( var part : text.split( "," ) )
      parsePart( assignments, part, resources );

    if ( assignments.isEmpty() )
      return null;

    var taskResources = new TaskResources();
    taskResources.m_assignments = assignments.values().toArray( new ParsedAssignment[0] );
    return taskResources;
  }

  /***************************************** parsePart ******************************************/
  private static void parsePart( Map<String, ParsedAssignment> assignments, String part, Resources resources )
  {
    if ( part.isBlank() )
      return;

    var parsed = parseAssignment( part, resources );
    var existing = assignments.get( parsed.tag() );
    if ( existing == null )
      assignments.put( parsed.tag(), parsed );
    else
      assignments.put( parsed.tag(), combine( existing, parsed ) );
  }

  /************************************** parseAssignment ***************************************/
  private static ParsedAssignment parseAssignment( String part, Resources resources )
  {
    var text = part.trim();
    var open = text.indexOf( '[' );
    var close = text.lastIndexOf( ']' );

    if ( open < 0 && close < 0 )
      return validateTag( Utils.clean( text ), QuantityMode.ALL_AVAILABLE, 0, DisplayStyle.NUMBER, resources );

    if ( open < 0 || close < 0 || close < open || close != text.length() - 1 || text.indexOf( '[', open + 1 ) >= 0
        || text.indexOf( ']', close + 1 ) >= 0 )
      throw new IllegalArgumentException( "Invalid resource assignment: " + text );

    var tag = Utils.clean( text.substring( 0, open ) );
    var quantity = Utils.clean( text.substring( open + 1, close ) );
    if ( quantity.isBlank() )
      throw new IllegalArgumentException( "Missing resource quantity: " + text );

    if ( quantity.equalsIgnoreCase( "all" ) )
      return validateTag( tag, QuantityMode.ALL_AVAILABLE, 0, DisplayStyle.NUMBER, resources );

    if ( quantity.endsWith( "%" ) )
      return validateTag( tag, QuantityMode.EXPLICIT, parsePercentQuantity( quantity ), DisplayStyle.PERCENT,
          resources );

    return validateTag( tag, QuantityMode.EXPLICIT, parseNumberQuantity( quantity ), DisplayStyle.NUMBER, resources );
  }

  /*************************************** validateTag ******************************************/
  private static ParsedAssignment validateTag( String tag, QuantityMode mode, int quantity100, DisplayStyle style,
      Resources resources )
  {
    if ( tag.isBlank() )
      throw new IllegalArgumentException( "Missing resource tag" );

    if ( resources.findByTag( tag ).isEmpty() )
      throw new IllegalArgumentException( "Unknown resource tag: " + tag );

    return new ParsedAssignment( tag, mode, quantity100, style );
  }

  /************************************ parseNumberQuantity *************************************/
  private static int parseNumberQuantity( String text )
  {
    if ( !NUMBER_QUANTITY.matcher( text ).matches() )
      throw new IllegalArgumentException( "Invalid resource quantity: " + text );

    var decimal = text.indexOf( '.' );
    long quantity100;
    if ( decimal < 0 )
      quantity100 = Long.parseLong( text ) * 100;
    else
    {
      var whole = Long.parseLong( text.substring( 0, decimal ) );
      var fraction = text.substring( decimal + 1 );
      if ( fraction.length() == 1 )
        fraction += "0";
      quantity100 = whole * 100 + Integer.parseInt( fraction );
    }

    return checkRange( quantity100, text );
  }

  /************************************ parsePercentQuantity ************************************/
  private static int parsePercentQuantity( String text )
  {
    var number = text.substring( 0, text.length() - 1 );
    if ( !PERCENT_QUANTITY.matcher( number ).matches() )
      throw new IllegalArgumentException( "Invalid resource percent quantity: " + text );

    return checkRange( Long.parseLong( number ), text );
  }

  /**************************************** checkRange ******************************************/
  private static int checkRange( long quantity100, String text )
  {
    if ( quantity100 < 0 || quantity100 > MAX_QUANTITY100 )
      throw new IllegalArgumentException( "Resource quantity out of range: " + text );

    return (int) quantity100;
  }

  /****************************************** combine *******************************************/
  private static ParsedAssignment combine( ParsedAssignment first, ParsedAssignment second )
  {
    if ( first.mode() == QuantityMode.ALL_AVAILABLE && second.mode() == QuantityMode.ALL_AVAILABLE )
      return first;

    if ( first.mode() == QuantityMode.ALL_AVAILABLE || second.mode() == QuantityMode.ALL_AVAILABLE )
      throw new IllegalArgumentException( "Cannot combine all-available and explicit quantities for: " + first.tag() );

    var quantity100 = (long) first.quantity100() + second.quantity100();
    checkRange( quantity100, first.tag() );
    var style = first.style() == DisplayStyle.PERCENT && second.style() == DisplayStyle.PERCENT ? DisplayStyle.PERCENT
        : DisplayStyle.NUMBER;

    return new ParsedAssignment( first.tag(), QuantityMode.EXPLICIT, (int) quantity100, style );
  }

  /******************************************* size **********************************************/
  public int size()
  {
    return m_assignments == null ? 0 : m_assignments.length;
  }

  /**************************************** assignments *****************************************/
  public List<Assignment> assignments()
  {
    if ( m_assignments == null )
      return List.of();

    var list = new ArrayList<Assignment>( m_assignments.length );

    for ( var assignment : m_assignments )
      list.add( new Assignment( assignment.tag(), assignment.mode() == QuantityMode.ALL_AVAILABLE,
          assignment.quantity100() / 100.0 ) );

    return list;
  }

  /**************************************** replaceTag ******************************************/
  public TaskResources replaceTag( String oldTag, String newTag )
  {
    if ( oldTag == null || oldTag.isBlank() || newTag == null || newTag.isBlank() || m_assignments == null )
      return this;

    if ( !containsTag( oldTag ) )
      return this;

    var assignments = new LinkedHashMap<String, ParsedAssignment>();

    for ( var assignment : m_assignments )
    {
      var tag = assignment.tag().equals( oldTag ) ? newTag : assignment.tag();
      var mapped = new ParsedAssignment( tag, assignment.mode(), assignment.quantity100(), assignment.style() );
      var existing = assignments.get( tag );

      if ( existing == null )
        assignments.put( tag, mapped );
      else
        assignments.put( tag, combine( existing, mapped ) );
    }

    return fromAssignments( assignments );
  }

  /***************************************** removeTag ******************************************/
  public TaskResources removeTag( String tag )
  {
    if ( tag == null || tag.isBlank() || m_assignments == null )
      return this;

    var assignments = new LinkedHashMap<String, ParsedAssignment>();
    boolean changed = false;

    for ( var assignment : m_assignments )
      if ( assignment.tag().equals( tag ) )
        changed = true;
      else
        assignments.put( assignment.tag(), assignment );

    return changed ? fromAssignments( assignments ) : this;
  }

  /**************************************** containsTag *****************************************/
  public boolean containsTag( String tag )
  {
    if ( tag == null || m_assignments == null )
      return false;

    for ( var assignment : m_assignments )
      if ( assignment.tag().equals( tag ) )
        return true;

    return false;
  }

  /************************************** fromAssignments ***************************************/
  private static TaskResources fromAssignments( LinkedHashMap<String, ParsedAssignment> assignments )
  {
    if ( assignments.isEmpty() )
      return null;

    var taskResources = new TaskResources();
    taskResources.m_assignments = assignments.values().toArray( new ParsedAssignment[0] );
    return taskResources;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    var sj = new StringJoiner( ", " );
    if ( m_assignments != null )
      for ( var assignment : m_assignments )
        sj.add( assignmentToString( assignment ) );

    return sj.toString();
  }

  /************************************** assignmentToString ************************************/
  private static String assignmentToString( ParsedAssignment assignment )
  {
    if ( assignment.mode() == QuantityMode.ALL_AVAILABLE )
      return assignment.tag();

    if ( assignment.style() == DisplayStyle.PERCENT )
      return assignment.tag() + "[" + assignment.quantity100() + "%]";

    return assignment.tag() + "[" + formatNumber( assignment.quantity100() ) + "]";
  }

  /**************************************** formatNumber ****************************************/
  private static String formatNumber( int quantity100 )
  {
    var whole = quantity100 / 100;
    var fraction = quantity100 % 100;

    if ( fraction == 0 )
      return Integer.toString( whole );
    if ( fraction % 10 == 0 )
      return whole + "." + fraction / 10;
    if ( fraction < 10 )
      return whole + ".0" + fraction;
    return whole + "." + fraction;
  }

}
