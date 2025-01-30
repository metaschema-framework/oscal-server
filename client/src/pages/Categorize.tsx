import { IonContent, IonPage } from '@ionic/react';
import { lazy } from 'react';
import PageHeader from '../components/common/PageHeader';
import SubMenu, { SubRoute } from '../components/SubMenu';
import FinalApproval from '../components/categorize/FinalApproval';
import SeniorReview from '../components/categorize/SeniorReview';
import RiskStrategy from '../components/categorize/RiskStrategy';
import ArchitectureAlignment from '../components/categorize/ArchitectureAlignment';
import SecurityDocumentation from '../components/categorize/SecurityDocumentation';
import SecurityImpact from '../components/categorize/SecurityImpact';
import InformationTypes from '../components/categorize/InformationTypes';
import SystemFunctions from '../components/categorize/SystemFunctions';
import SystemBoundaries from '../components/categorize/SystemBoundaries';
import SystemCharacteristics from '../components/categorize/SystemCharacteristics';

interface RMFStep {
  component: React.ComponentType;
  slug: string;
  title: string;
  description: string;
  frameworks?: string[];
}

const Categorize: React.FC = () => {
  const steps: SubRoute[] = [
    {
      component: SystemCharacteristics,
      slug: 'c-1-system-characteristics',
      description: 'Document and describe the characteristics of the system',
    },
    {
      component: SystemBoundaries,
      slug: 'c1-system-boundaries',
      description: 'Define and document the system boundaries and interfaces'
    },
    {
      component: SystemFunctions,
      slug: 'c1-system-functions',
      description: 'Document system functions, capabilities, and mission support'
    },
    {
      component: InformationTypes,
      slug: 'c2.a-information-types',
      description: 'Identify and categorize information types processed by the system',
    },
    {
      component: SecurityImpact,
      slug: 'c2.b-security-impact',
      description: 'Determine security impact levels for confidentiality, integrity, and availability',
    },
    {
      component: SecurityDocumentation,
      slug: 'c2.c-documentation',
      description: 'Document categorization in security, privacy, and SCRM plans',
    },
    {
      component: ArchitectureAlignment,
      slug: 'c2.d-architecture-alignment',
      description: 'Verify consistency with enterprise architecture and organizational missions'
    },
    {
      component: RiskStrategy,
      slug: 'c2.e-risk-strategy',
      description: 'Ensure categorization reflects organizational risk management strategy'
    },
    {
      component: SeniorReview,
      slug: 'c3.a-senior-review',
      description: 'Review of security categorization results by senior leadership'
    },
    {
      component: FinalApproval,
      slug: 'c3.b-approval',
      description: 'Obtain senior leadership approval for categorization decisions'
    }
  ];

  return (
    <IonPage>
      <PageHeader base='categorize' title="RMF Categorize Phase" />
      <IonContent>
        <SubMenu 
          routes={steps.map(step => ({
            component: step.component,
            slug: step.slug,
          }))}
        />
      </IonContent>
    </IonPage>
  );
};

export default Categorize;