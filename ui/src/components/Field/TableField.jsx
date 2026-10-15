/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

import {faChevronDown} from '@fortawesome/free-solid-svg-icons/faChevronDown';
import {faChevronRight} from '@fortawesome/free-solid-svg-icons/faChevronRight';
import {faMagnifyingGlass} from '@fortawesome/free-solid-svg-icons/faMagnifyingGlass';
import {faSort} from '@fortawesome/free-solid-svg-icons/faSort';
import {faSortDown} from '@fortawesome/free-solid-svg-icons/faSortDown';
import {faSortUp} from '@fortawesome/free-solid-svg-icons/faSortUp';
import {faTimes} from '@fortawesome/free-solid-svg-icons/faTimes';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React, { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { Hint } from '../Hint/Hint';
import './TableField.css';
import { getKey } from './helpers';

const CustomTableCell = ({ field, isFirstCell, onCollapseToggle, fieldComponent: FieldComponent }) => {

  if (!field.data) {
    return field.level === 1 ? <th />:<td className={`kg-cell_level_${field.level}`} />;
  }

  const handleClick = () => onCollapseToggle(field.collectionIndex, !field.isCollectionCollapsed);

  if (field.level === 1) {
    return (
      <td>
        {isFirstCell && field.isCollectionCollapsible && (
          <button onClick={handleClick}><FontAwesomeIcon icon={field.isCollectionCollapsed?faChevronRight:faChevronDown} /></button>
        )}
        <FieldComponent name={field.name} data={field.data} mapping={field.mapping} />
        {isFirstCell && field.isCollectionASubset && (
          <Hint className="kg-cell-hint" value={`The represented tissue samples are the subset used in this ${field.type?field.type.toLowerCase():'dataset'}`} />
        )}
      </td>
    );
  }
  return (
    <td className={`kg-cell_level_${field.level}`}><FieldComponent name={field.name} data={field.data} mapping={field.mapping} /></td>
  );
};

const CustomTableRow = ({ row, onCollapseToggle, fieldComponent }) => {
  const collapse = row[0].isCollectionCollapsed && row[0].level !== 1;
  return(
    <tr className={collapse?'row-hidden':null}>
      {row.map((field, index) => <CustomTableCell key={`${field.name}-${index}`} isFirstCell={!index} field={field} onCollapseToggle={onCollapseToggle} fieldComponent={fieldComponent} />)}
    </tr>
  );
};

const normalizeCells = (fields, data, type, level, collectionIndex, isCollectionCollapsed, isCollectionCollapsible, isCollectionASubset) => Object.entries(fields)
  .map(([name, field]) => ({
    name: name,
    data: data && data[name],
    mapping: {...field, hideLabel:true},
    type: type,
    level: level,
    isCollectionCollapsed: isCollectionCollapsed,
    collectionIndex: collectionIndex,
    isCollectionCollapsible: isCollectionCollapsible,
    isCollectionASubset: isCollectionASubset
  }));

const normalizeRows = (list, collapsedRowIndexes) => list.reduce((acc, item, index) => {
  const hasChildren = item.data && item.data.children;
  const isCollapsible = hasChildren && item.data.collapsible;
  const isSubset = hasChildren && item.data.subset;
  const isCollectionCollapsed = isCollapsible && !!collapsedRowIndexes[index];
  if (item.isObject) {
    acc.push(normalizeCells(item.mapping.children, item.data, item.type, 1, index, isCollectionCollapsed, isCollapsible, isSubset));
    if (hasChildren) {
      item.data.children.forEach(child => {
        acc.push(normalizeCells(item.mapping.children, child, item.type, 2, index, isCollectionCollapsed, false));
        if(child.children) {
          child.children.forEach(c => {
            acc.push(normalizeCells(item.mapping.children, c, item.type, 3, index, isCollectionCollapsed, false));
          });
        }
      });
    }
  } else {
    acc.push(item);
  }
  return acc;
}, []);


const filterRows = table => {
  const visibleColumns = table.reduce((acc, row) => {
    row.forEach(cell => {
      if (cell.data) {
        acc[cell.name] = true;
      }
    });
    return acc;
  }, {});
  return table.map(row =>
    row.reduce((acc, cell) => {
      if (visibleColumns[cell.name]) {
        acc.push(cell);
      }
      return acc;
    }, []));
};

const getSortValueFromData = data => {
  if (data === undefined || data === null) {
    return '';
  }
  if (typeof data === 'string' || typeof data === 'number' || typeof data === 'boolean') {
    return String(data);
  }
  if (Array.isArray(data)) {
    return data.map(getSortValueFromData).filter(Boolean).join(' ');
  }
  if (typeof data === 'object') {
    if (data.value !== undefined && data.value !== null && data.value !== '') {
      return String(data.value);
    }
    if (data.url) {
      return String(data.url);
    }
    if (data.reference) {
      return String(data.reference);
    }
  }
  return '';
};

const getCellSortValue = cell => getSortValueFromData(cell?.data);

const compareSortValues = (left, right, direction) => {
  const leftText = String(left ?? '').trim();
  const rightText = String(right ?? '').trim();
  const leftNumber = Number(leftText);
  const rightNumber = Number(rightText);
  const leftIsNumber = leftText !== '' && !Number.isNaN(leftNumber);
  const rightIsNumber = rightText !== '' && !Number.isNaN(rightNumber);

  let result;
  if (leftIsNumber && rightIsNumber) {
    result = leftNumber - rightNumber;
  } else {
    result = leftText.localeCompare(rightText, undefined, { numeric: true, sensitivity: 'base' });
  }

  return direction === 'desc' ? -result : result;
};

const partitionRowsIntoGroups = rows => {
  const groups = [];
  let currentGroup = null;

  rows.forEach(row => {
    const level = row[0]?.level;
    if (level === 1 || level === undefined) {
      if (currentGroup) {
        groups.push(currentGroup);
      }
      currentGroup = [row];
    } else if (currentGroup) {
      currentGroup.push(row);
    } else {
      groups.push([row]);
    }
  });

  if (currentGroup) {
    groups.push(currentGroup);
  }

  return groups;
};

const sortTableRows = (rows, sortState) => {
  if (sortState.columnIndex === null || !rows.length) {
    return rows;
  }

  const groups = partitionRowsIntoGroups(rows);

  return [...groups]
    .sort((groupA, groupB) => {
      const leftCell = groupA[0]?.[sortState.columnIndex];
      const rightCell = groupB[0]?.[sortState.columnIndex];
      return compareSortValues(
        getCellSortValue(leftCell),
        getCellSortValue(rightCell),
        sortState.direction
      );
    })
    .flat();
};

const filterRowsByColumnFilters = (rows, columnFilters) => {
  const activeColumnIndexes = Object.keys(columnFilters).filter(index => columnFilters[index]?.length > 0);
  if (!activeColumnIndexes.length) {
    return rows;
  }

  const groups = partitionRowsIntoGroups(rows);
  return groups
    .filter(group =>
      activeColumnIndexes.every(columnIndex => {
        const selectedValues = columnFilters[columnIndex];
        return group.some(row => {
          const cellValue = getCellSortValue(row[columnIndex]);
          return selectedValues.includes(cellValue);
        });
      })
    )
    .flat();
};

const ActiveFiltersBar = ({ rows, columnFilters, onRemove, onClearAll }) => {
  const activeFilters = Object.keys(columnFilters).reduce((acc, columnIndex) => {
    const column = rows[0]?.[columnIndex];
    if (!column) {
      return acc;
    }
    const label = column.mapping?.label ?? column.name;
    columnFilters[columnIndex].forEach(value => {
      acc.push({ columnIndex: Number(columnIndex), label, value });
    });
    return acc;
  }, []);

  if (!activeFilters.length) {
    return null;
  }

  return (
    <div className="kgs-table__active-filters">
      <span className="kgs-table__active-filters-label">Filtered by:</span>
      {activeFilters.map(({ columnIndex, label, value }, index) => {
        const displayValue = value.trim() || '(empty)';
        return (
          <span key={`${columnIndex}-${value}-${index}`} className="kgs-table__active-filter-chip">
            <span className="kgs-table__active-filter-text">{label}: {displayValue}</span>
            <button
              type="button"
              className="kgs-table__active-filter-remove"
              onClick={() => onRemove(columnIndex, value)}
              aria-label={`Remove filter ${label}: ${displayValue}`}
            >
              <FontAwesomeIcon icon={faTimes} aria-hidden="true" />
            </button>
          </span>
        );
      })}
      <button type="button" className="kgs-table__active-filters-clear" onClick={onClearAll}>
        Clear all
      </button>
    </div>
  );
};

const ColumnFilterDropdown = ({ rows, columnIndex, selectedValues, onChange, onClose, toggleButtonRef }) => {
  const [searchText, setSearchText] = useState('');
  const dropdownRef = useRef(null);
  const [position, setPosition] = useState(null);

  useLayoutEffect(() => {
    const button = toggleButtonRef?.current;
    if (!button) {
      return;
    }
    const rect = button.getBoundingClientRect();
    const dropdownWidth = 320;
    let left = rect.left + window.scrollX;
    if (left + dropdownWidth > window.innerWidth) {
      left = Math.max(8, rect.right + window.scrollX - dropdownWidth);
    }
    setPosition({
      top: rect.bottom + window.scrollY + 6,
      left
    });
  }, [toggleButtonRef]);

  const distinctValues = useMemo(() => {
    const map = new Map();
    rows.forEach(row => {
      const cell = row[columnIndex];
      const rawValue = getCellSortValue(cell);
      const displayText = rawValue.trim() || '(empty)';
      if (!map.has(displayText)) {
        map.set(displayText, { value: rawValue, displayText, count: 0 });
      }
      map.get(displayText).count += 1;
    });
    return Array.from(map.values()).sort((a, b) =>
      a.displayText.localeCompare(b.displayText, undefined, { numeric: true, sensitivity: 'base' })
    );
  }, [rows, columnIndex]);

  const filteredValues = useMemo(() => {
    const normalized = searchText.trim().toLowerCase();
    if (!normalized) {
      return distinctValues;
    }
    return distinctValues.filter(item => item.displayText.toLowerCase().includes(normalized));
  }, [distinctValues, searchText]);

  useEffect(() => {
    const handleClickOutside = event => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        const button = toggleButtonRef?.current;
        if (!button || !button.contains(event.target)) {
          onClose();
        }
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [onClose, toggleButtonRef]);

  const toggleValue = value => {
    if (selectedValues.includes(value)) {
      onChange(selectedValues.filter(v => v !== value));
    } else {
      onChange([...selectedValues, value]);
    }
  };

  const selectAll = () => onChange(filteredValues.map(item => item.value));
  const clearAll = () => onChange([]);

  return (
    <div
      className="kgs-table__filter-dropdown"
      ref={dropdownRef}
      style={position ? { top: position.top, left: position.left } : { visibility: 'hidden' }}
    >
      <div className="kgs-table__filter-dropdown-header">
        <input
          type="search"
          className="kgs-table__filter-dropdown-input"
          placeholder="Search values..."
          value={searchText}
          onChange={event => setSearchText(event.target.value)}
          autoFocus
        />
      </div>
      <div className="kgs-table__filter-dropdown-actions">
        <button type="button" onClick={selectAll}>Select all</button>
        <button type="button" onClick={clearAll}>Clear</button>
      </div>
      <div className="kgs-table__filter-dropdown-list">
        {filteredValues.length === 0 ? (
          <div className="kgs-table__filter-dropdown-empty">No values found</div>
        ) : (
          filteredValues.map(({ value, displayText, count }) => (
            <label key={displayText} className="kgs-table__filter-dropdown-item">
              <input
                type="checkbox"
                checked={selectedValues.includes(value)}
                onChange={() => toggleValue(value)}
              />
              <span className="kgs-table__filter-dropdown-text">{displayText}</span>
              <span className="kgs-table__filter-dropdown-count">{count}</span>
            </label>
          ))
        )}
      </div>
    </div>
  );
};

const TableHeader = ({ column, columnIndex, sortState, onSort, rows, selectedValues, onFilterChange, isFilterOpen, onToggleFilter, showControls }) => {
  const label = column.mapping?.label ?? column.name;
  const isSorted = sortState.columnIndex === columnIndex;
  const sortIcon = !isSorted
    ? faSort
    : (sortState.direction === 'asc' ? faSortUp : faSortDown);
  const hasFilter = selectedValues.length > 0;
  const toggleButtonRef = useRef(null);

  const handleSortClick = () => onSort(columnIndex);

  return (
    <th
      scope="col"
      aria-sort={isSorted ? (sortState.direction === 'asc' ? 'ascending' : 'descending') : 'none'}
    >
      {showControls ? (
        <div className="kgs-table__header-content">
          <button
            type="button"
            className={`kgs-table__sort-button${isSorted ? ' is-sorted' : ''}`}
            onClick={handleSortClick}
            aria-label={`Sort by ${label}${isSorted ? `, ${sortState.direction === 'asc' ? 'ascending' : 'descending'}` : ''}`}
          >
            <span className="kgs-table__sort-label">{label}</span>
            <FontAwesomeIcon icon={sortIcon} className="kgs-table__sort-icon" aria-hidden="true" />
          </button>
          <button
            ref={toggleButtonRef}
            type="button"
            className={`kgs-table__filter-toggle${hasFilter ? ' is-active' : ''}`}
            onClick={onToggleFilter}
            aria-label={`Filter ${label}`}
            aria-expanded={isFilterOpen}
          >
            <FontAwesomeIcon icon={faMagnifyingGlass} aria-hidden="true" />
          </button>
        </div>
      ) : (
        <span className="kgs-table__sort-label">{label}</span>
      )}
      {isFilterOpen && (
        <ColumnFilterDropdown
          rows={rows}
          columnIndex={columnIndex}
          selectedValues={selectedValues}
          onChange={onFilterChange}
          onClose={onToggleFilter}
          toggleButtonRef={toggleButtonRef}
        />
      )}
    </th>
  );
};

const TableFieldComponent = ({ list, fieldComponent }) => {
  const initialState = list.reduce((acc, _, index) => {
    acc[index] = true;
    return acc;
  }, {});
  const [collapsedRowIndexes, setCollapsedRowIndexes] = useState(initialState);
  const [sortState, setSortState] = useState({ columnIndex: null, direction: 'asc' });
  const [columnFilters, setColumnFilters] = useState({});
  const [openFilterColumnIndex, setOpenFilterColumnIndex] = useState(null);
  const [showScrollHint, setShowScrollHint] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);
  const scrollRef = useRef(null);

  const rows = filterRows(normalizeRows(list, collapsedRowIndexes));
  const showTableControls = rows.length > 1;
  const sortedRows = useMemo(
    () => showTableControls ? sortTableRows(rows, sortState) : rows,
    [rows, sortState, showTableControls]
  );
  const filteredRows = useMemo(
    () => filterRowsByColumnFilters(sortedRows, columnFilters),
    [sortedRows, columnFilters]
  );

  const updateScrollState = useCallback(() => {
    const el = scrollRef.current;
    if (!el) {
      return;
    }
    const hasOverflow = el.scrollWidth > el.clientWidth + 1;
    const atStart = el.scrollLeft < 8;
    setShowScrollHint(hasOverflow && atStart);
    setIsScrolled(el.scrollLeft > 0);
  }, []);

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) {
      return undefined;
    }

    updateScrollState();

    const observer = typeof ResizeObserver !== 'undefined'
      ? new ResizeObserver(updateScrollState)
      : null;
    observer?.observe(el);

    el.addEventListener('scroll', updateScrollState, { passive: true });
    window.addEventListener('resize', updateScrollState);

    return () => {
      observer?.disconnect();
      el.removeEventListener('scroll', updateScrollState);
      window.removeEventListener('resize', updateScrollState);
    };
  }, [sortedRows, updateScrollState]);

  if (!rows.length || !rows[0].length) {
    return null;
  }

  const onCollapseToggle = (index, collapse) => {
    const values = {...collapsedRowIndexes};
    if (collapse) {
      values[index] = true;
    } else {
      delete values[index];
    }
    setCollapsedRowIndexes(values);
  };

  const handleSort = columnIndex => {
    setSortState(current => {
      if (current.columnIndex === columnIndex) {
        return {
          columnIndex,
          direction: current.direction === 'asc' ? 'desc' : 'asc'
        };
      }
      return { columnIndex, direction: 'asc' };
    });
  };

  const handleColumnFilterChange = (columnIndex, values) => {
    setColumnFilters(current => {
      const next = { ...current };
      if (values.length) {
        next[columnIndex] = values;
      } else {
        delete next[columnIndex];
      }
      return next;
    });
  };

  const handleRemoveFilter = (columnIndex, value) => {
    setColumnFilters(current => {
      const nextValues = current[columnIndex].filter(v => v !== value);
      const next = { ...current };
      if (nextValues.length) {
        next[columnIndex] = nextValues;
      } else {
        delete next[columnIndex];
      }
      return next;
    });
  };

  const handleClearAllFilters = () => {
    setColumnFilters({});
  };

  const handleToggleFilter = columnIndex => {
    setOpenFilterColumnIndex(current => current === columnIndex ? null : columnIndex);
  };

  return (
    <div className="kgs-table-wrap">
      {(showScrollHint || isScrolled) && (
        <p className="kgs-table-scroll-hint" style={{visibility: isScrolled ? "hidden" : "visible"}} aria-hidden="true">
          Swipe to see more →
        </p>
      )}
      <ActiveFiltersBar
        rows={rows}
        columnFilters={columnFilters}
        onRemove={handleRemoveFilter}
        onClearAll={handleClearAllFilters}
      />
      <div
        ref={scrollRef}
        className={`kgs-table-scroll${isScrolled ? ' is-scrolled' : ''}`}
      >
        <table className={`table${showTableControls ? '' : ' kgs-table--no-controls'}`}>
        <thead>
          <tr>
            {rows[0].map((column, columnIndex) => (
              <TableHeader
                key={`${column.name}-${columnIndex}`}
                column={column}
                columnIndex={columnIndex}
                sortState={sortState}
                onSort={handleSort}
                rows={rows}
                selectedValues={columnFilters[columnIndex] || []}
                onFilterChange={values => handleColumnFilterChange(columnIndex, values)}
                isFilterOpen={showTableControls && openFilterColumnIndex === columnIndex}
                onToggleFilter={() => handleToggleFilter(columnIndex)}
                showControls={showTableControls}
              />
            ))}
          </tr>
        </thead>
        <tbody>
          {filteredRows.map((row, index) => (
            <CustomTableRow
              key={`${index}`}
              row={row}
              onCollapseToggle={onCollapseToggle}
              fieldComponent={fieldComponent}
            />
          ))}
        </tbody>
      </table>
      </div>
    </div>
  );
};

class TableField extends React.Component {
  getItems = () => {
    const { items, mapping, type } = this.props;
    const convertedItem = Array.isArray(items)?items:[items];
    return convertedItem.map((item, idx) => ({
      isObject: !!item.children,
      key: getKey(item, idx),
      data: item.children?item.children:item,
      mapping: mapping,
      type: type
    }));
  };

  render() {
    const { fieldComponent } = this.props;
    return (
      <TableFieldComponent list={this.getItems()} fieldComponent={fieldComponent} />
    );
  }
}

export default TableField;
