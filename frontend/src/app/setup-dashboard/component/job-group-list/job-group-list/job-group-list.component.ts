import {Component} from '@angular/core';
import {JobGroupDTO} from "../../../../shared/model/job-group.model";
import {Observable} from 'rxjs';
import {JobGroupService} from "../../../../shared/service/rest/job-group.service";

@Component({
  selector: 'osm-job-group-list',
  templateUrl: './job-group-list.component.html',
  styleUrls: ['./job-group-list.component.css']
})
export class JobGroupListComponent {
  jobGroupList$: Observable<JobGroupDTO[]>;

  constructor(private jobGroupService: JobGroupService) {
    this.jobGroupList$ = this.jobGroupService.jobGroups$
  }
}