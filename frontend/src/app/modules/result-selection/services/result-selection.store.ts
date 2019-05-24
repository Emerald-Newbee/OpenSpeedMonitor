import {Caller, ResultSelectionCommand} from "../models/result-selection-command.model";
import {BehaviorSubject, ReplaySubject} from "rxjs";
import {MeasuredEvent} from "../../../models/measured-event.model";
import {Location} from "../../../models/location.model";
import {Connectivity} from "../../../models/connectivity.model";
import {Injectable} from "@angular/core";
import {ApplicationWithPages, SelectableApplication} from "../../../models/application.model";
import {ResponseWithLoadingState} from "../../../models/response-with-loading-state.model";
import {MeasurandGroup, SelectableMeasurand} from "../../../models/measurand.model";
import {ResultSelectionService, URL} from "./result-selection.service";

export enum UiComponent {
  APPLICATION, PAGE_LOCATION_CONNECTIVITY, MEASURAND
}

@Injectable()
export class ResultSelectionStore {
  from: Date;
  to: Date;
  _resultSelectionCommand$: BehaviorSubject<ResultSelectionCommand>;

  applications$: BehaviorSubject<SelectableApplication[]> = new BehaviorSubject([]);
  applicationsAndPages$: BehaviorSubject<ApplicationWithPages[]> = new BehaviorSubject([]);
  eventsAndPages$: BehaviorSubject<MeasuredEvent[]> = new BehaviorSubject([]);
  locationsAndBrowsers$: BehaviorSubject<Location[]> = new BehaviorSubject([]);
  connectivities$: BehaviorSubject<Connectivity[]> = new BehaviorSubject([]);
  loadTimes$: ReplaySubject<ResponseWithLoadingState<MeasurandGroup>> = new ReplaySubject(1);
  userTimings$: ReplaySubject<ResponseWithLoadingState<MeasurandGroup>> = new ReplaySubject(1);
  heroTimings$: ReplaySubject<ResponseWithLoadingState<MeasurandGroup>> = new ReplaySubject(1);
  requestCounts$: ReplaySubject<ResponseWithLoadingState<MeasurandGroup>> = new ReplaySubject(1);
  requestSizes$: ReplaySubject<ResponseWithLoadingState<MeasurandGroup>> = new ReplaySubject(1);
  percentages$: ReplaySubject<ResponseWithLoadingState<MeasurandGroup>> = new ReplaySubject(1);
  resultCount$: ReplaySubject<string> = new ReplaySubject<string>(1);
  oldResult: ResultSelectionCommand;
  selectedConnectivityChanged: boolean = false;

  constructor(private resultSelectionService: ResultSelectionService){
    let defaultFrom = new Date();
    let defaultTo = new Date();
    defaultFrom.setDate(defaultTo.getDate() - 3);
    this._resultSelectionCommand$ = new BehaviorSubject({from:defaultFrom, to: defaultTo, caller: Caller.EventResult});
  }

  setSelectedTimeFrame(selectedTimeFrame: Date[]) {
    this.setResultSelectionCommand({...this.resultSelectionCommand, from: selectedTimeFrame[0], to:selectedTimeFrame[1]});
  }

  setSelectedApplications(ids: number[]){
    this.setResultSelectionCommand({...this.resultSelectionCommand, jobGroupIds: ids});
  }

  setSelectedPagesAndMeasurands(pageIds: number[], measuredEventIds: number[]){
    this.setResultSelectionCommand({...this.resultSelectionCommand, pageIds: pageIds, measuredEventIds: measuredEventIds});
  }

  setSelectedBrowserAndLocation(browserIds: number[], locationIds: number[]){
    this.setResultSelectionCommand({...this.resultSelectionCommand, browserIds: browserIds, locationIds: locationIds});
  }

  setSelectedConnectivities(connectivities: number[]){
    this.selectedConnectivityChanged = true;
    this.setResultSelectionCommand({...this.resultSelectionCommand, selectedConnectivities: connectivities});
  }

  get resultSelectionCommand(){
    return this._resultSelectionCommand$.getValue();
  }

  setResultSelectionCommand(newState: ResultSelectionCommand){
    this._resultSelectionCommand$.next(newState);
  }

  registerComponent(component: UiComponent){
    if(component === UiComponent.APPLICATION) {
      this._resultSelectionCommand$.subscribe(state => {
        this.loadSelectableApplications(state);
      });
    } else if(component === UiComponent.PAGE_LOCATION_CONNECTIVITY) {
      this._resultSelectionCommand$.subscribe(state => {
        this.loadSelectableEventsAndPages(state);
        this.loadSelectableLocationsAndBrowsers(state);
        if(!this.selectedConnectivityChanged){
          this.loadSelectableConnectivities(state);
        }
        this.selectedConnectivityChanged = false;
      });
    } else if(component === UiComponent.MEASURAND) {
      this._resultSelectionCommand$.subscribe(state => {
        this.loadMeasurands(state);
        this.loadUserTimings(state);
        this.loadHeroTimings(state);
      });
    }
  }

  loadSelectableApplications(resultSelectionCommand: ResultSelectionCommand): void {
    this.resultSelectionService.fetchResultSelectionData<SelectableApplication[]>(resultSelectionCommand, URL.APPLICATIONS)
      .subscribe(next => this.applications$.next(next));
  }

  loadSelectableApplicationsAndPages(resultSelectionCommand: ResultSelectionCommand): void {
    this.resultSelectionService.fetchResultSelectionData<ApplicationWithPages[]>(resultSelectionCommand, URL.APPLICATIONS_AND_PAGES)
      .subscribe(next => this.applicationsAndPages$.next(next));
  }

  loadSelectableEventsAndPages(resultSelectionCommand: ResultSelectionCommand): void {
    this.resultSelectionService.fetchResultSelectionData<MeasuredEvent[]>(resultSelectionCommand, URL.EVENTS_AND_PAGES)
      .subscribe(next => this.eventsAndPages$.next(next));
  }

  loadSelectableLocationsAndBrowsers(resultSelectionCommand: ResultSelectionCommand): void {
    this.resultSelectionService.fetchResultSelectionData<Location[]>(resultSelectionCommand, URL.LOCATIONS_AND_BROWSERS)
      .subscribe(next => this.locationsAndBrowsers$.next(next));
  }

  loadSelectableConnectivities(resultSelectionCommand: ResultSelectionCommand): void {
    this.resultSelectionService.fetchResultSelectionData<Connectivity[]>(resultSelectionCommand, URL.CONNECTIVITIES)
      .subscribe(next => this.connectivities$.next(next));
  }

  loadMeasurands(resultSelectionCommand: ResultSelectionCommand): void {
    this.resultSelectionService.updateMeasurands(resultSelectionCommand).subscribe((groups: MeasurandGroup[]) => {
      if (groups) {
        groups.forEach((group: MeasurandGroup) => {
          let responseWithLoadingState: ResponseWithLoadingState<MeasurandGroup> = {
            isLoading: false,
            data: {
              name: "frontend.de.iteratec.isr.measurand.group." + group.name,
              values: group.values.map(measurand => ({
                name: "frontend.de.iteratec.isr.measurand." + measurand.name,
                id: measurand.id
              }))
            }
          };
          let concerningSubject$ = this.getDefaultSubjectByMeasurandGroup(group.name);
          if (concerningSubject$) {
            concerningSubject$.next(responseWithLoadingState);
          }
        });
      }
    });
  }

  loadUserTimings(resultSelectionCommand: ResultSelectionCommand): void {
    this.resultSelectionService.fetchResultSelectionData<SelectableMeasurand[]>(resultSelectionCommand, URL.USER_TIMINGS)
      .subscribe(next => {
      const groupName: string = "User Timings";
      let responseWithLoadingState: ResponseWithLoadingState<MeasurandGroup> = {
        isLoading: false,
        data: {name: groupName, values: next}
      };
      this.userTimings$.next(responseWithLoadingState);
    });
  }

  loadHeroTimings(resultSelectionCommand: ResultSelectionCommand): void {
    this.resultSelectionService.fetchResultSelectionData<SelectableMeasurand[]>(resultSelectionCommand, URL.HERO_TIMINGS)
      .subscribe(next => {
      const groupName: string = "Hero Timings";
      let responseWithLoadingState: ResponseWithLoadingState<MeasurandGroup> = {
        isLoading: false,
        data: {name: groupName, values: next}
      };
      this.heroTimings$.next(responseWithLoadingState);
    });
  }

  loadResultCount(resultSelectionCommand: ResultSelectionCommand): void {
    this.resultSelectionService.fetchResultSelectionData<string>(resultSelectionCommand, URL.RESULT_COUNT)
      .subscribe(next => this.resultCount$.next(next));
  }

  private getDefaultSubjectByMeasurandGroup(name: string): ReplaySubject<ResponseWithLoadingState<MeasurandGroup>> | undefined {
    let subject$: ReplaySubject<ResponseWithLoadingState<MeasurandGroup>>;
    switch (name) {
      case "LOAD_TIMES":
        subject$ = this.loadTimes$;
        break;
      case "REQUEST_COUNTS":
        subject$ = this.requestCounts$;
        break;
      case "REQUEST_SIZES":
        subject$ = this.requestSizes$;
        break;
      case "PERCENTAGES":
        subject$ = this.percentages$;
        break;
    }
    return subject$;
  }

}
