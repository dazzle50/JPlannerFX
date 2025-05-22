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

import javafx.scene.paint.Paint;
import rjc.table.view.Colours;

/*************************************************************************************************/
/****************************** Scheduling type of task within plan ******************************/
/*************************************************************************************************/

public enum TaskType
{
  // early as possible - fixed duration
  ASAP_FIXED_DURATION( "ASAP - duration", true, false, false, false ),

  // early as possible - fixed work
  ASAP_FIXED_WORK( "ASAP - work", false, false, false, true ),

  // start on - fixed duration
  START_ON_DURATION( "Start on - duration", true, true, false, false ),

  // start on - fixed work
  START_ON_WORK( "Start on - work", false, true, false, true ),

  // fixed period
  FIXED_PERIOD( "Fixed period", false, true, true, false );

  private String  m_text;     // display name for type
  private boolean m_duration; // is duration field user editable
  private boolean m_start;    // is start field user editable
  private boolean m_end;      // is end field user editable
  private boolean m_work;     // is work field user editable

  /***************************************** constructor *****************************************/
  TaskType( String text, boolean duration, boolean start, boolean end, boolean work )
  {
    // define task-type display text and editable fields
    m_text = text;
    m_duration = duration;
    m_start = start;
    m_end = end;
    m_work = work;
  }

  /************************************* durationBackground **************************************/
  public Paint durationBackground()
  {
    // return colour dependent if cell is user editable
    return m_duration ? Colours.CELL_DEFAULT_BACKGROUND : Colours.CELL_DISABLED_BACKGROUND;
  }

  /*************************************** startBackground ***************************************/
  public Paint startBackground()
  {
    // return colour dependent if cell is user editable
    return m_start ? Colours.CELL_DEFAULT_BACKGROUND : Colours.CELL_DISABLED_BACKGROUND;
  }

  /**************************************** endBackground ****************************************/
  public Paint endBackground()
  {
    // return colour dependent if cell is user editable
    return m_end ? Colours.CELL_DEFAULT_BACKGROUND : Colours.CELL_DISABLED_BACKGROUND;
  }

  /*************************************** workBackground ****************************************/
  public Paint workBackground()
  {
    // return colour dependent if cell is user editable
    return m_work ? Colours.CELL_DEFAULT_BACKGROUND : Colours.CELL_DISABLED_BACKGROUND;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return display name for the enum constants
    return m_text;
  }

}
