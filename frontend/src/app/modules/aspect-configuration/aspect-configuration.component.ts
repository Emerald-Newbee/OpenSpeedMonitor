import {Component, OnDestroy, OnInit} from '@angular/core';
import {ApplicationService} from "../../services/application.service";
import {ActivatedRoute, ParamMap} from "@angular/router";
import {Page} from "../../models/page.model";
import {combineLatest, Observable} from "rxjs";
import {ExtendedPerformanceAspect, PerformanceAspect} from "../../models/perfomance-aspect.model";
import {Application} from "../../models/application.model";
import {AspectConfigurationService} from "./services/aspect-configuration.service";
import {BrowserInfoDto} from "../../models/browser.model";
import {map} from "rxjs/operators";

@Component({
  selector: 'osm-aspect-configuration',
  templateUrl: './aspect-configuration.component.html',
  styleUrls: ['./aspect-configuration.component.scss']
})
export class AspectConfigurationComponent implements OnInit, OnDestroy {

  application$: Observable<Application>;
  page$: Observable<Page>;

  performanceAspects$: Observable<ExtendedPerformanceAspect[]>;

  constructor(private route: ActivatedRoute, private applicationService: ApplicationService, private aspectConfService: AspectConfigurationService) {
    this.application$ = applicationService.selectedApplication$;
    this.page$ = applicationService.selectedPage$;
    this.prepareExtensionOfAspects();
  }

  ngOnInit() {
    this.getBrowserInfos();
    this.route.paramMap.subscribe((params: ParamMap) => {
      this.getApplication(params.get('applicationId'));
      this.getPage(params.get('pageId'));
    });
    // this.activateDebugging();
  }

  ngOnDestroy(): void {

  }

  private activateDebugging() {
    this.application$.subscribe(app => console.log(`app=${JSON.stringify(app)}`));
    this.page$.subscribe(page => console.log(`page=${JSON.stringify(page)}`));
    this.applicationService.performanceAspectsForPage$.subscribe(aspects => console.log(`aspect=${JSON.stringify(aspects)}`));
    this.aspectConfService.browserInfos$.subscribe(browserInfos => console.log(`browserInfos=${browserInfos}`));
    this.performanceAspects$.subscribe(extendedAspects => console.log(`extendedAspects=${JSON.stringify(extendedAspects)}`))
  }

  private prepareExtensionOfAspects() {
    debugger;
    this.performanceAspects$ = combineLatest(this.applicationService.performanceAspectsForPage$, this.aspectConfService.browserInfos$).pipe(
      map(([aspects, browserInfos]: [PerformanceAspect[], BrowserInfoDto[]]) => {
        const extendedAspects = this.extendAspects(aspects, browserInfos);
        return extendedAspects;
      })
    )
  }

  private extendAspects(aspects: PerformanceAspect[], browserInfos: BrowserInfoDto[]) {
    const extendedAspects: ExtendedPerformanceAspect[] = [];
    if (aspects.length > 0 && browserInfos.length > 0) {
      aspects.forEach((aspect: PerformanceAspect) => {
        const additionalInfos = browserInfos.filter((browserInfo: BrowserInfoDto) => browserInfo.browserId == aspect.browserId)
        if (additionalInfos.length == 1) {
          extendedAspects.push({...aspect, ...additionalInfos[0]})
        }
      });
    }
    return extendedAspects;
  }

  getApplication(applicationId: string) {
    this.aspectConfService.loadApplication(applicationId).subscribe((app: Application) => {
      this.applicationService.selectedApplication$.next(app);
    })
  }

  getPage(pageId: string) {
    this.aspectConfService.loadPage(pageId).subscribe((page: Page) => {
      this.applicationService.selectedPage$.next(page);
    })
  }

  getBrowserInfos() {
    this.aspectConfService.loadBrowserInfos()
  }
}
