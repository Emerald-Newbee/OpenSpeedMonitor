import {Component, OnDestroy} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {combineLatest, Observable, Subject} from 'rxjs';
import {filter, map, takeUntil} from 'rxjs/operators';
import {ApplicationService} from '../../services/application.service';
import {Application} from '../../models/application.model';
import {PageMetricsDto} from "./models/page-metrics.model";
import {ApplicationCsi, ApplicationCsiById} from "../../models/application-csi.model";
import {Csi} from "../../models/csi.model";
import {FailingJobStatistic} from "./models/failing-job-statistic.model";
import {ResultSelectionService} from "../result-selection/services/result-selection.service";

@Component({
  selector: 'osm-application-dashboard',
  templateUrl: './application-dashboard.component.html',
  styleUrls: ['./application-dashboard.component.scss']
})
export class ApplicationDashboardComponent implements OnDestroy {
  applications$: Observable<Application[]>;
  selectedApplication: Application;
  destroyed$ = new Subject<void>();
  pages$: Observable<PageMetricsDto[]>;
  applicationCsi$: Observable<ApplicationCsi>;
  recentCsiValue$: Observable<Csi>;
  hasConfiguration$: Observable<boolean>;
  isLoading$: Observable<boolean>;
  failingJobStatistic$: Observable<FailingJobStatistic>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private applicationService: ApplicationService,
    private resultSelectionService: ResultSelectionService
  ) {
    this.pages$ = this.applicationService.metrics$;
    this.applications$ = applicationService.applications$.pipe(
      filter(response => !response.isLoading && !!response.data),
      map(response => response.data)
    );
    this.applicationCsi$ = applicationService.selectSelectedApplicationCsi();
    this.isLoading$ = combineLatest(applicationService.applicationCsiById$, applicationService.selectedApplication$)
      .pipe(map(([applicationCsiById, selectedApplication]: [ApplicationCsiById, Application]) => applicationCsiById.isLoading && !applicationCsiById[selectedApplication.id]));
    this.recentCsiValue$ = this.applicationCsi$
      .pipe(map((applicationCsi: ApplicationCsi) => applicationCsi.recentCsi()));
    this.hasConfiguration$ = this.applicationCsi$
      .pipe(map((applicationCsi: ApplicationCsi) => applicationCsi.hasCsiConfiguration));
    combineLatest(this.route.paramMap, this.applications$)
      .pipe(takeUntil(this.destroyed$))
      .subscribe(([navParams, applications]) => this.handleNavigation(navParams.get('applicationId'), applications));
    this.applicationService.loadApplications();
    this.failingJobStatistic$ = this.applicationService.failingJobStatistics$;
  }

  private handleNavigation(applicationId: string, applications: Application[]) {
    if (!applicationId) {
      this.updateApplication(applications[0]);
      return;
    }
    this.selectedApplication = this.findApplicationById(applications, applicationId);
    if (this.selectedApplication) {
      this.applicationService.updateSelectedApplication(this.selectedApplication);
      this.resultSelectionService.updateApplications([this.selectedApplication]);
    }
  }

  ngOnDestroy() {
    this.destroyed$.next(null);
    this.destroyed$.complete();
  }

  updateApplication(application: Application) {
    this.router.navigate(['/applicationDashboard', application.id]);
  }

  private findApplicationById(applications: Application[], applicationId: string): Application {
    return applications.find(application => application.id == Number(applicationId));
  }

}
