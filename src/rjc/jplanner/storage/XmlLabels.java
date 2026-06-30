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

package rjc.jplanner.storage;

/*************************************************************************************************/
/**************** Holds the XML labels used in saving & loading JPlannerFX files *****************/
/*************************************************************************************************/

public class XmlLabels
{
  public static final String VERSION             = "1.0";
  public static final String ENCODING            = "UTF-8";

  // 'JPlanner' labels
  public static final String FORMAT              = "2026";
  public static final String XML_JPLANNER        = "JPlanner";
  public static final String XML_FORMAT          = "format";
  public static final String XML_SAVEUSER        = "saved-by";
  public static final String XML_SAVEWHEN        = "saved-when";
  public static final String XML_SAVENAME        = "saved-name";
  public static final String XML_SAVEWHERE       = "saved-where";

  // 'plan' labels
  public static final String XML_DAY_DATA        = "days-data";
  public static final String XML_CAL_DATA        = "calendars-data";
  public static final String XML_RES_DATA        = "resources-data";
  public static final String XML_TASK_DATA       = "tasks-data";
  public static final String XML_PLAN_DATA       = "plan-data";
  public static final String XML_TITLE           = "title";
  public static final String XML_START           = "start";
  public static final String XML_DT_FORMAT       = "datetime-format";
  public static final String XML_D_FORMAT        = "date-format";
  public static final String XML_NOTES           = "notes";

  // 'calendar' labels
  public static final String XML_CALENDAR        = "calendar";
  public static final String XML_ID              = "id";
  public static final String XML_NAME            = "name";
  public static final String XML_ANCHOR          = "anchor";
  public static final String XML_NORMAL          = "normal";
  public static final String XML_DAY             = "day";
  public static final String XML_EXCEPTION       = "exception";
  public static final String XML_DATE            = "date";

  // 'resource' labels
  public static final String XML_COUNT           = "count";
  public static final String XML_RESOURCE        = "resource";
  public static final String XML_INITIALS        = "initials";
  public static final String XML_ORG             = "org";
  public static final String XML_GROUP           = "group";
  public static final String XML_ROLE            = "role";
  public static final String XML_ALIAS           = "alias";
  public static final String XML_END             = "end";
  public static final String XML_AVAIL           = "availability";
  public static final String XML_COST            = "cost";
  public static final String XML_COMMENT         = "comment";

  // 'day' labels
  public static final String XML_WORK            = "work";
  public static final String XML_PERIOD          = "period";

  // 'task' labels
  public static final String XML_TASK            = "task";
  public static final String XML_PREDECESSORS    = "predecessors";
  public static final String XML_PREDS           = "preds";
  public static final String XML_DURATION        = "duration";
  public static final String XML_RESOURCES       = "resources";
  public static final String XML_TYPE            = "type";
  public static final String XML_PRIORITY        = "priority";
  public static final String XML_DEADLINE        = "deadline";
  public static final String XML_INDENT          = "indent";

  // 'display' labels
  public static final String XML_DISPLAY_DATA    = "display-data";
  public static final String XML_WINDOW          = "window";
  public static final String XML_X               = "x";
  public static final String XML_Y               = "y";
  public static final String XML_TAB             = "tab";
  public static final String XML_TASKS_TABLE     = "tasks-table";
  public static final String XML_RESOURCES_TABLE = "resources-table";
  public static final String XML_CALENDARS_TABLE = "calendars-table";
  public static final String XML_DAYTYPES_TABLE  = "days-table";
  public static final String XML_COLUMNS         = "columns";
  public static final String XML_COLUMN          = "column";
  public static final String XML_ROWS            = "rows";
  public static final String XML_ROW             = "row";
  public static final String XML_WIDTH           = "width";
  public static final String XML_HEIGHT          = "height";
  public static final String XML_GANTT           = "gantt";
  public static final String XML_SCALE           = "scale";
  public static final String XML_INTERVAL        = "interval";
  public static final String XML_MSPP            = "mspp";
  public static final String XML_NONWORKING      = "nonworking";
  public static final String XML_CURRENT         = "current";
  public static final String XML_STRETCH         = "stretch";
  public static final String XML_SPLITTER        = "splitter";
  public static final String XML_COLLAPSED       = "collapsed";
  public static final String XML_POSITION        = "position";
  public static final String XML_UNDOSTACK       = "undostack";
  public static final String XML_VISIBLE         = "visible";
  public static final String XML_SCROLL          = "scroll";
  public static final String XML_SELECTED        = "selected";
}
