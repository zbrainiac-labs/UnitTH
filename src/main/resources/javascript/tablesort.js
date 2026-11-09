/**
 * Modern Table Sort - Enhanced version with ES6+ features
 * Based on the original TableSort implementation
 */
class ModernTableSort {
  constructor(options = {}) {
    this.table = null;
    this.columnHeaders = [];
    this.tableRows = [];
    this.currentColumn = 0; // Default sorting on first column
    this.sortDirection = 'asc'; // 'asc' or 'desc'
    this.previousColumn = null;

    // Configuration options
    this.options = {
      enableSorting: true,
      // null = keep the generated row order (runs newest first, names already sorted)
      defaultColumn: null,
      sortableClass: 'sortable',
      ascClass: 'sort-asc',
      descClass: 'sort-desc',
      unsortedClass: 'sort-none',
      ...options
    };
  }

  /**
   * Initialize table sorting functionality
   * @param {string|HTMLElement} tableSelector - Table element or selector
   * @returns {boolean} Success status
   */
  init(tableSelector) {
    // Get table element
    if (typeof tableSelector === 'string') {
      this.table = document.getElementById(tableSelector) || document.querySelector(tableSelector);
    } else if (tableSelector instanceof HTMLElement) {
      this.table = tableSelector;
    }

    if (!this.table) {
      console.warn('TableSort: Table not found');
      return false;
    }

    // Get column headers
    this.columnHeaders = Array.from(this.table.querySelectorAll('th'));

    if (this.columnHeaders.length === 0) {
      console.warn('TableSort: No table headers found');
      return false;
    }

    // Setup click handlers for sortable columns
    this.setupColumnHandlers();

    // Initial sort if specified
    if (this.options.defaultColumn !== null) {
      this.sortByColumn(this.options.defaultColumn, 'asc', false);
    }

    return true;
  }

  /**
   * Setup click handlers for column headers
   */
  setupColumnHandlers() {
    this.columnHeaders.forEach((header, index) => {
      // Skip non-sortable columns
      if (header.dataset.sortable === 'false' || header.textContent.trim() === 'SPREAD') {
        return;
      }

      // Add sortable class
      header.classList.add(this.options.sortableClass);
      header.style.cursor = 'pointer';

      // Add click handler
      header.addEventListener('click', (e) => {
        e.preventDefault();
        this.handleColumnClick(index, header);
      });

      // Add initial unsorted class
      header.classList.add(this.options.unsortedClass);
    });
  }

  /**
   * Handle column header click
   * @param {number} columnIndex - Index of clicked column
   * @param {HTMLElement} header - Header element
   */
  handleColumnClick(columnIndex, header) {
    if (!this.options.enableSorting) return;

    // Determine sort direction
    let direction = 'asc';
    if (this.currentColumn === columnIndex && this.sortDirection === 'asc') {
      direction = 'desc';
    }

    this.sortByColumn(columnIndex, direction, true);
  }

  /**
   * Sort table by specified column
   * @param {number} columnIndex - Column to sort by
   * @param {string} direction - Sort direction ('asc' or 'desc')
   * @param {boolean} updateUI - Whether to update UI classes
   */
  sortByColumn(columnIndex, direction = 'asc', updateUI = true) {
    // Get all data rows (exclude header)
    const tbody = this.table.querySelector('tbody') || this.table;
    this.tableRows = Array.from(tbody.querySelectorAll('tr'));

    if (this.tableRows.length === 0) return;

    // Get column type from header attribute
    const header = this.columnHeaders[columnIndex];
    const columnType = header?.dataset.type || this.detectColumnType(columnIndex);

    // Sort rows
    this.tableRows.sort((rowA, rowB) => {
      const cellA = this.getCellValue(rowA, columnIndex);
      const cellB = this.getCellValue(rowB, columnIndex);

      let comparison = this.compareValues(cellA, cellB, columnType);

      return direction === 'desc' ? -comparison : comparison;
    });

    // Update DOM
    this.updateTableDOM();

    // Update UI state
    if (updateUI) {
      this.updateColumnClasses(columnIndex, direction);
    }

    // Update internal state
    this.previousColumn = this.currentColumn;
    this.currentColumn = columnIndex;
    this.sortDirection = direction;

    // Dispatch custom event
    this.dispatchSortEvent(columnIndex, direction);
  }

  /**
   * Get cell value from row
   * @param {HTMLElement} row - Table row
   * @param {number} columnIndex - Column index
   * @returns {string} Cell text content
   */
  getCellValue(row, columnIndex) {
    const cell = row.cells[columnIndex];
    if (!cell) return '';

    // Get text content, handling nested elements
    let value = cell.textContent || cell.innerText || '';

    // Handle input elements
    const input = cell.querySelector('input');
    if (input) {
      value = input.value || value;
    }

    return value.trim();
  }

  /**
   * Detect column data type based on content
   * @param {number} columnIndex - Column index
   * @returns {string} Detected type
   */
  detectColumnType(columnIndex) {
    const sampleSize = Math.min(5, this.tableRows.length);
    const samples = [];

    for (let i = 0; i < sampleSize; i++) {
      const value = this.getCellValue(this.tableRows[i], columnIndex);
      if (value) samples.push(value);
    }

    if (samples.length === 0) return 'string';

    // Check for numbers
    if (samples.every(val => !isNaN(parseFloat(val)) && isFinite(val))) {
      return 'number';
    }

    // Check for percentages
    if (samples.every(val => val.includes('%'))) {
      return 'percent';
    }

    // Check for dates
    if (samples.every(val => !isNaN(Date.parse(val)))) {
      return 'date';
    }

    // Check for currency/floats with special formatting
    if (samples.some(val => /[\(\)$?��]/.test(val))) {
      return 'float';
    }

    return 'string';
  }

  /**
   * Compare two values based on type
   * @param {string} a - First value
   * @param {string} b - Second value
   * @param {string} type - Data type
   * @returns {number} Comparison result
   */
  compareValues(a, b, type) {
    if (a === b) return 0;

    switch (type) {
      case 'number':
        return this.compareNumbers(a, b);

      case 'percent':
        return this.comparePercentages(a, b);

      case 'date':
        return this.compareDates(a, b);

      case 'float':
        return this.compareFloats(a, b);

      case 'input_text':
        return this.compareInputText(a, b);

      default:
        return this.compareStrings(a, b);
    }
  }

  /**
   * Compare numeric values
   */
  compareNumbers(a, b) {
    const numA = parseFloat(a) || 0;
    const numB = parseFloat(b) || 0;
    return numA - numB;
  }

  /**
   * Compare percentage values
   */
  comparePercentages(a, b) {
    const numA = parseFloat(a.replace('%', '')) || 0;
    const numB = parseFloat(b.replace('%', '')) || 0;
    return numA - numB;
  }

  /**
   * Compare date values
   */
  compareDates(a, b) {
    const dateA = new Date(a);
    const dateB = new Date(b);
    return dateA - dateB;
  }

  /**
   * Compare float values with special formatting
   */
  compareFloats(a, b) {
    // Remove formatting characters and extract number
    const cleanA = a.replace(/[^\d.-]/g, '');
    const cleanB = b.replace(/[^\d.-]/g, '');
    const numA = parseFloat(cleanA) || 0;
    const numB = parseFloat(cleanB) || 0;
    return numA - numB;
  }

  /**
   * Compare input text (strip HTML tags)
   */
  compareInputText(a, b) {
    const cleanA = a.replace(/<[^>]*>/g, '').trim();
    const cleanB = b.replace(/<[^>]*>/g, '').trim();
    return cleanA.localeCompare(cleanB);
  }

  /**
   * Compare string values
   */
  compareStrings(a, b) {
    return a.localeCompare(b, undefined, { numeric: true, sensitivity: 'base' });
  }

  /**
   * Update table DOM with sorted rows
   */
  updateTableDOM() {
    const tbody = this.table.querySelector('tbody') || this.table;

    // Remove existing rows
    this.tableRows.forEach(row => row.remove());

    // Add sorted rows
    this.tableRows.forEach(row => tbody.appendChild(row));
  }

  /**
   * Update column header classes
   * @param {number} activeColumn - Currently sorted column
   * @param {string} direction - Sort direction
   */
  updateColumnClasses(activeColumn, direction) {
    this.columnHeaders.forEach((header, index) => {
      // Remove all sort classes
      header.classList.remove(this.options.ascClass, this.options.descClass, this.options.unsortedClass);

      if (index === activeColumn) {
        // Add active sort class
        const sortClass = direction === 'asc' ? this.options.ascClass : this.options.descClass;
        header.classList.add(sortClass);
      } else {
        // Add unsorted class
        header.classList.add(this.options.unsortedClass);
      }
    });
  }

  /**
   * Dispatch custom sort event
   * @param {number} columnIndex - Sorted column
   * @param {string} direction - Sort direction
   */
  dispatchSortEvent(columnIndex, direction) {
    const event = new CustomEvent('tableSorted', {
      detail: {
        column: columnIndex,
        direction: direction,
        header: this.columnHeaders[columnIndex],
        table: this.table
      }
    });

    this.table.dispatchEvent(event);
  }

  /**
   * Get current sort state
   * @returns {Object} Sort state
   */
  getSortState() {
    return {
      column: this.currentColumn,
      direction: this.sortDirection,
      columnName: this.columnHeaders[this.currentColumn]?.textContent || ''
    };
  }

  /**
   * Enable/disable sorting
   * @param {boolean} enabled - Enable state
   */
  setSortingEnabled(enabled) {
    this.options.enableSorting = enabled;

    this.columnHeaders.forEach(header => {
      header.style.cursor = enabled ? 'pointer' : 'default';
      if (enabled) {
        header.classList.add(this.options.sortableClass);
      } else {
        header.classList.remove(this.options.sortableClass);
      }
    });
  }

  /**
   * Reset table to original order
   */
  reset() {
    // This would require storing original order - implement if needed
    console.warn('Reset functionality requires storing original row order');
  }

  /**
   * Destroy the table sort instance
   */
  destroy() {
    this.columnHeaders.forEach(header => {
      header.replaceWith(header.cloneNode(true)); // Remove event listeners
      header.classList.remove(
        this.options.sortableClass,
        this.options.ascClass,
        this.options.descClass,
        this.options.unsortedClass
      );
      header.style.cursor = '';
    });

    this.table = null;
    this.columnHeaders = [];
    this.tableRows = [];
  }

  /**
   * Static method to create and initialize table sort
   * @param {string|HTMLElement} tableSelector - Table selector or element
   * @param {Object} options - Configuration options
   * @returns {ModernTableSort} Table sort instance
   */
  static create(tableSelector, options = {}) {
    const instance = new ModernTableSort(options);
    instance.init(tableSelector);
    return instance;
  }
}

// For backward compatibility, create global TableSort function
window.TableSort = function() {
  const instance = new ModernTableSort();
  return {
    init: (tableId) => instance.init(tableId)
  };
};
