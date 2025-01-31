import React from 'react';
import { IonAccordionGroup, IonAccordion, IonItem, IonLabel } from '@ionic/react';
import { OscalPackage, Catalog, Profile, ComponentDefinition, SystemSecurityPlanSSP, SecurityAssessmentPlanSAP, SecurityAssessmentResultsSAR, PlanOfActionAndMilestonesPOAM } from '../../types';
import { RenderMetadata } from './RenderMetadata';
import { RenderControls } from './RenderControls';
import { RenderGroups } from './RenderGroups';
import { RenderBackMatter } from './RenderBackMatter';
import { RenderImplementedRequirements } from './RenderImplementedRequirements';
import { RenderComponents } from './RenderComponents';
import { RenderSystemCharacteristics } from './RenderSystemCharacteristics';
import './RenderOscal.css';

interface RenderOscalProps {
  document: OscalPackage;
}

type DocumentType = keyof Pick<OscalPackage, 'catalog' | 'profile' | 'component-definition' | 'system-security-plan' | 
  'assessment-plan' | 'assessment-results' | 'plan-of-action-and-milestones'>;

const getDocumentType = (doc: OscalPackage): DocumentType | null => {
  if (doc.catalog) return 'catalog';
  if (doc.profile) return 'profile';
  if (doc['component-definition']) return 'component-definition';
  if (doc['system-security-plan']) return 'system-security-plan';
  if (doc['assessment-plan']) return 'assessment-plan';
  if (doc['assessment-results']) return 'assessment-results';
  if (doc['plan-of-action-and-milestones']) return 'plan-of-action-and-milestones';
  return null;
};

const renderError = (message: string) => (
  <IonAccordionGroup>
    <IonAccordion value="error">
      <IonItem slot="header" color="light">
        <IonLabel>Error</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <p>{message}</p>
      </div>
    </IonAccordion>
  </IonAccordionGroup>
);

const renderCatalog = (content: Catalog[]) => (
  <IonAccordionGroup>
    {content[0]?.metadata && <RenderMetadata metadata={content[0].metadata} />}
    {content[0]?.groups && <RenderGroups groups={content[0].groups} />}
    {content[0]?.controls && <RenderControls controls={content[0].controls} />}
    {content[0]?.['back-matter'] && <RenderBackMatter backMatter={content[0]['back-matter']} />}
  </IonAccordionGroup>
);

const renderProfile = (content: Profile) => (
  <IonAccordionGroup>
    {content.metadata && <RenderMetadata metadata={content.metadata} />}
    {content.imports && (
      <IonAccordion value="imports">
        <IonItem slot="header" color="light">
          <IonLabel>Imports</IonLabel>
        </IonItem>
        <div className="ion-padding" slot="content">
          {content.imports.map((imp, index) => (
            <div key={index}>
              <p>Href: {imp.href}</p>
            </div>
          ))}
        </div>
      </IonAccordion>
    )}
    {content['back-matter'] && <RenderBackMatter backMatter={content['back-matter']} />}
  </IonAccordionGroup>
);

const renderComponentDefinition = (content: ComponentDefinition) => (
  <IonAccordionGroup>
    {content.metadata && <RenderMetadata metadata={content.metadata} />}
    {content.components && <RenderComponents components={content.components.map(comp => ({
      ...comp,
      status: { state: 'operational' }
    }))} />}
    {content['back-matter'] && <RenderBackMatter backMatter={content['back-matter']} />}
  </IonAccordionGroup>
);

const renderSystemSecurityPlan = (content: SystemSecurityPlanSSP) => (
  <IonAccordionGroup>
    {content.metadata && <RenderMetadata metadata={content.metadata} />}
    {content['system-characteristics'] && (
      <RenderSystemCharacteristics characteristics={content['system-characteristics']} />
    )}
    {content['system-implementation']?.components && (
      <RenderComponents components={content['system-implementation'].components} />
    )}
    {content['control-implementation'] && (
      <RenderImplementedRequirements implementation={content['control-implementation']} />
    )}
    {content['back-matter'] && <RenderBackMatter backMatter={content['back-matter']} />}
  </IonAccordionGroup>
);

const renderAssessmentPlan = (content: SecurityAssessmentPlanSAP) => (
  <IonAccordionGroup>
    {content?.metadata && <RenderMetadata metadata={content.metadata} />}
    {content?.['back-matter'] && <RenderBackMatter backMatter={content['back-matter']} />}
  </IonAccordionGroup>
);

const renderAssessmentResults = (content: SecurityAssessmentResultsSAR) => (
  <IonAccordionGroup>
    {content?.metadata && <RenderMetadata metadata={content.metadata} />}
    {content?.['back-matter'] && <RenderBackMatter backMatter={content['back-matter']} />}
  </IonAccordionGroup>
);

const renderPOAM = (content: PlanOfActionAndMilestonesPOAM) => (
  <IonAccordionGroup>
    {content?.metadata && <RenderMetadata metadata={content.metadata} />}
    {content?.['back-matter'] && <RenderBackMatter backMatter={content['back-matter']} />}
  </IonAccordionGroup>
);

export const RenderOscal: React.FC<RenderOscalProps> = ({ document }) => {
  const type = getDocumentType(document);
  if (!type) {
    return renderError("Unsupported document type");
  }

  const content = document[type];
  if (!content) {
    return renderError("Invalid document content");
  }

  switch (type) {
    case 'catalog':
      return renderCatalog(content as Catalog[]);
    case 'profile':
      return renderProfile(content as Profile);
    case 'component-definition':
      return renderComponentDefinition(content as ComponentDefinition);
    case 'system-security-plan':
      return renderSystemSecurityPlan(content as SystemSecurityPlanSSP);
    case 'assessment-plan':
      return renderAssessmentPlan(content as SecurityAssessmentPlanSAP);
    case 'assessment-results':
      return renderAssessmentResults(content as SecurityAssessmentResultsSAR);
    case 'plan-of-action-and-milestones':
      return renderPOAM(content as PlanOfActionAndMilestonesPOAM);
    default:
      return renderError("Unsupported document type");
  }
};
