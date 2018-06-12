import { Injectable } from '@angular/core';
import {HttpClient, HttpParams} from "@angular/common/http";
import {JobGroup} from "../../model/job-group.model";
import {Observable, ReplaySubject} from "rxjs/index";
import {IPage} from "../../../common/model/page.model";
import {IJobGroupToPagesMapping} from "../../../common/model/job-group-to-page-mapping.model";

@Injectable({
  providedIn: 'root'
})
export class PageService {

  public pages$ = new ReplaySubject<IJobGroupToPagesMapping[]>(1);


  constructor(private http: HttpClient) {
    this.updatePages()
  }


  updatePages() {
    this.http.get<IJobGroupToPagesMapping[]>("page/getPagesForActiveJobGroups")
      .subscribe(next => this.pages$.next(next), error => this.handleError(error));
  }

  handleError(error: any){
    console.log(error);
  }
}
