export class Util {
  /**
   * VALIDATATING
   */
  static isValidVal(item: any): any {
    return !Util.isBlank(item) ? item : ''
  }

  // undefined, null, NaN, 0, "" (empty string), and false
  // isBlank(" ") = false
  static isBlank(item: any): boolean {
    if (!item) {
      return true
    } else if (Array.isArray(item)) {
      return item.length === 0
    } else if (typeof item === 'string') {
      return item.trim().length === 0
    }
    return false
  }

  static areEqual(array1: Array<any>, array2: Array<any>): boolean {
    if (array1.length === array2.length) {
      return array1.every((element, index) => {
        if (element === array2[index]) {
          return true
        }

        return false
      })
    }

    return false
  }

  static isJson(str: any) {
    try {
      JSON.parse(str)
    } catch (e) {
      return false
    }
    return true
  }

  static isURL(input: string): boolean {
    // Regular expression for URL validation
    const urlRegex = /^(https?|ftp):\/\/[^\s/$.?#].[^\s]*$/
    return urlRegex.test(input)
  }

  /**
   * FORMATTING
   */

  /**
   * Returns the parameter of the given date type as a string with a certain pattern.
   * @param date
   * @returns YYYY-MM-DD hh:mm:ss
   */
  static formatDateTime(date: Date | null): string | null {
    if (!date) {
      return null
    }

    const year = new Intl.DateTimeFormat('en', { year: 'numeric' }).format(date)
    const month = new Intl.DateTimeFormat('en', { month: '2-digit' }).format(date)
    const day = new Intl.DateTimeFormat('en', { day: '2-digit' }).format(date)

    const hour = date.getHours().toString().padStart(2, '0')
    const minute = date.getMinutes().toString().padStart(2, '0')
    const second = date.getSeconds().toString().padStart(2, '0')

    return `${year}-${month}-${day} ${hour}:${minute}:${second}`
  }

  static formatLargeNumber(number: number, decimalPlaces = 10): string | null {
    if (number) {
      return number.toFixed(decimalPlaces).replace(/\.?0+$/, '') // Remove trailing zeros
    } else {
      return null
    }
  }

  static formatNumberFractionGroup(minimumFractionDigits: number, maximumFractionDigits: number): any {
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: minimumFractionDigits,
      maximumFractionDigits: maximumFractionDigits,
      useGrouping: true,
    })
  }

  static formatNumberGroup(): any {
    return new Intl.NumberFormat('en-US', {
      useGrouping: true,
    })
  }

  /**
   * converts a given Unix timestamp to a Date object.
   * @param {number} unixTimestamp - Unix timestamps in seconds
   * @returns {Date | null} the converted Date object or null if the conversion failed
   */
  static formatUnixTimestampToDate(timestamp: number): Date | null {
    try {
      const date = new Date(timestamp * 1000) // Convert seconds to milliseconds
      return date
    } catch (error) {
      console.error('Failed to convert timestamp to date:', error)
      return null
    }
  }

  /**
   * INSERTING
   */
  static insertComma(item: string): string {
    if (!Util.isBlank(item)) {
      return ', '
    } else {
      return ''
    }
  }

  static insertBR(item: string): string {
    if (!Util.isBlank(item)) {
      return '<br/>'
    } else {
      return ''
    }
  }

  /**
   * CONVERTING
   */
  static arrayToString(array: Array<any>, separator: string): string {
    if (array) {
      return array.join(separator)
    } else {
      return ''
    }
  }

  /**
   * EXTRACTING
   */
  static translateCode(code: string, codeList: Array<any>): string {
    if (code && codeList && codeList.length > 0) {
      return codeList.find((c) => c.codeName === code).description
    } else {
      return ''
    }
  }
  static userIdConversion(item: string): string {
    if (item && item.indexOf('\\') !== -1) {
      return item.split('\\')[1]
    } else {
      return item
    }
  }

  /**
   * SPLITING
   */
  static splitStringByCharCamelCase(inString: string): string {
    return !Util.isBlank(inString) ? inString.replace(/([a-z0-9])([A-Z])/g, '$1 $2') : ''
  }

  /**
   * MISCELLANEOUS
   */
  static flattenArray(inputArray: any[]): any[] {
    const result: any[] = []

    const flattenHelper = function (arr: any[]) {
      for (const element of arr) {
        if (Array.isArray(element) && Array.isArray(element[0])) {
          flattenHelper(element)
        } else {
          result.push(element)
        }
      }
    }

    flattenHelper(inputArray)
    return result
  }

  static openRequestedPopup(url: string, windowsName: string, windowFeatures?: string): void {
    if (Util.isBlank(windowFeatures)) windowFeatures = 'toolbar=0,location=0,menubar=0'
    window.open(url, windowsName, windowFeatures)
  }

  static async delay(ms: number): Promise<any> {
    return new Promise((resolve) => setTimeout(resolve, ms))
  }

  static deepCopy(obj: any): any {
    return JSON.parse(JSON.stringify(obj)) as typeof obj
  }
}
