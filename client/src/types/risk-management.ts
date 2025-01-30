import { Property, Link, Remarks } from '../types';

export interface RiskManagementStrategy {
  uuid: string;
  methodology: string;
  tolerance: string;
  mitigation: string;
  monitoring: string;
  props?: Property[];
  links?: Link[];
  remarks?: Remarks;
}

export interface RiskAssessment {
  uuid: string;
  threats: string[];
  vulnerabilities: string[];
  impactAssessment: string;
  riskDetermination: string;
  props?: Property[];
  links?: Link[];
  remarks?: Remarks;
}

export interface ControlBaseline {
  uuid: string;
  href: string;
  remarks?: string;
  props?: Property[];
  links?: Link[];
}

export interface CommonControl {
  uuid: string;
  'control-id': string;
  props?: Property[];
  links?: Link[];
  'set-parameters'?: any[];
  'responsible-roles'?: any[];
  statements?: any[];
  'by-components'?: any[];
  remarks?: Remarks;
}
