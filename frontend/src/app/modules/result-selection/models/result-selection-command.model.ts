export interface ResultSelectionCommandDTO {
  from: Date;
  to: Date;
  caller?: Caller;
  jobGroupIds?: number[];
  pageIds?: number[];
  measuredEventIds?: number[];
  browserIds?: number[];
  locationIds?: number[];
  selectedConnectivities?: string[];
}

export class ResultSelectionCommand implements ResultSelectionCommandDTO {
  from: Date;
  to: Date;
  caller?: Caller;
  jobGroupIds?: number[];
  pageIds?: number[];
  measuredEventIds?: number[];
  browserIds?: number[];
  locationIds?: number[];
  selectedConnectivities?: string[];

  constructor (dto: ResultSelectionCommandDTO) {
    this.from = dto.from;
    this.to = dto.to;
    this.caller = dto.caller;
    this.jobGroupIds = dto.jobGroupIds;
    this.pageIds = dto.pageIds;
    this.measuredEventIds = dto.measuredEventIds;
    this.browserIds = dto.browserIds;
    this.locationIds = dto.locationIds;
    this.selectedConnectivities = dto.selectedConnectivities;
  }
}

export enum Caller {
  CsiAggregation,
  EventResult
}
