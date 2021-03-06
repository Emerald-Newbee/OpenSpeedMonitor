import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {MeasurandSelectComponent} from './measurand-select.component';
import {SharedMocksModule} from "../../../../testing/shared-mocks.module";
import {ResultSelectionService} from "../../services/result-selection.service";

describe('MeasurandSelectComponent', () => {
  let component: MeasurandSelectComponent;
  let fixture: ComponentFixture<MeasurandSelectComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [MeasurandSelectComponent],
      imports: [SharedMocksModule],
      providers: [
        ResultSelectionService
      ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MeasurandSelectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
