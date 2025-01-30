import React from 'react';
import { IonAccordion, IonAccordionGroup, IonItem, IonLabel, IonList, IonButton, IonChip, IonNote, IonBadge } from '@ionic/react';
import { EntityType, ResponsibleRole, SSPControlImplementation, SystemSecurityPlanSSP } from '../types';

interface RenderOscalProps {
  document: Record<string, any>;
}

const getDocumentType = (doc: Record<string, any>): string => {
  if (doc.catalog) return 'catalog';
  if (doc.profile) return 'profile';
  if (doc['component-definition']) return 'component-definition';
  if (doc['system-security-plan']) return 'system-security-plan';
  if (doc['assessment-plan']) return 'assessment-plan';
  if (doc['assessment-results']) return 'assessment-results';
  if (doc['plan-of-action-and-milestones']) return 'plan-of-action-and-milestones';
  return 'unknown';
};

const renderProps = (props: any[]) => {
  if (!props?.length) return null;
  return (
    <div className="props-container">
      {props.map((prop, index) => (
        <IonChip key={index} className="prop-chip" color="primary">
          <IonLabel>
            <strong>{prop.name}:</strong> {prop.value}
          </IonLabel>
          {prop.class && (
            <IonBadge color="light" className="prop-class">
              {prop.class}
            </IonBadge>
          )}
        </IonChip>
      ))}
    </div>
  );
};

// Add styles for nested content and props
const styles = document.createElement('style');
styles.textContent = `
  .props-container {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin: 8px 0;
  }
  .prop-chip {
    --background: var(--ion-color-primary-tint);
    --color: var(--ion-color-primary-contrast);
  }
  .prop-class {
    margin-left: 4px;
    font-size: 0.8em;
  }
  .nested-content {
    margin-left: 16px;
    border-left: 2px solid var(--ion-color-light);
  }
  .nested-header {
    margin-bottom: 8px;
    color: var(--ion-color-medium);
  }
  .group-item {
    margin-bottom: 16px;
  }
  .group-title {
    color: var(--ion-color-dark);
    margin: 0 0 8px 0;
  }
  .statement-id {
    color: var(--ion-color-medium);
    font-size: 0.9em;
    margin-bottom: 4px;
  }
  .status-container {
    margin: 8px 0;
  }
  .status-remarks {
    color: var(--ion-color-medium);
    font-style: italic;
    margin: 4px 0;
  }
  .roles-container {
    margin: 8px 0;
  }
  .by-component {
    margin: 12px 0;
    padding: 8px;
    background: var(--ion-color-light-tint);
    border-radius: 4px;
  }
  .by-component-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }
`;
document.head.appendChild(styles);

const openResource = (href: string, mediaType: string) => {
  // Handle different media types appropriately
  if (mediaType.startsWith('image/')) {
    window.open(href, '_blank');
  } else if (mediaType === 'application/pdf') {
    window.open(href, '_blank');
  } else if (mediaType.startsWith('text/')) {
    window.open(href, '_blank');
  } else if (mediaType.startsWith('video/')) {
    window.open(href, '_blank');
  } else {
    // Default behavior for unknown types
    window.open(href, '_blank');
  }
};

const renderImplementedRequirements = (implementation:SSPControlImplementation ) => {

  return (
    <IonAccordion value="implemented-requirements">
      <IonItem slot="header" color="light">
        <IonLabel>Implemented Requirements</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {implementation['implemented-requirements'].map((req, index: number) => (
            <IonItem key={req.uuid || index}>
            <IonLabel>
                <h2>Control: {req['control-id']}</h2>
                {req.remarks && <p>{req.remarks}</p>}
                {req.props && renderProps(req.props)}
                {req.statements?.map((statement, stmtIndex: number) => (
                  <div key={stmtIndex} className="nested-content ion-padding">
                    <h3 className="statement-id">{statement['statement-id']}</h3>
                    {statement.remarks && <p>{statement.remarks}</p>}
                    {statement.props && renderProps(statement.props)}
                    {statement['by-components']?.map((byComp, compIndex: number) => (
                  <div key={compIndex} className="by-component">
                    <div className="by-component-header">
                      <IonChip color="success">
                        <IonLabel>Component: {byComp['component-uuid']}</IonLabel>
                      </IonChip>
                      {byComp['implementation-status'] && (
                        <IonChip color="warning">
                          <IonLabel>Status: {byComp['implementation-status'].state}</IonLabel>
                        </IonChip>
                      )}
                    </div>
                    {byComp.description && <p>{byComp.description}</p>}
                    {byComp.props && renderProps(byComp.props)}
                    {byComp['responsible-roles']&&byComp['responsible-roles']?.length > 0 && (
                      <div className="roles-container">
                        <h3 className="nested-header">Responsible Roles</h3>
                        {byComp['responsible-roles'].map((role, roleIndex: number) => (
                          <IonChip key={roleIndex} color="medium">
                            <IonLabel>{role['role-id']}</IonLabel>
                          </IonChip>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
                  </div>
                ))}

              </IonLabel>
            </IonItem>
          ))}
        </IonList>
      </div>
    </IonAccordion>
  );
};

const renderBackMatter = (backMatter: any) => {
  if (!backMatter?.resources?.length) return null;
  return (
    <IonAccordion value="resources">
      <IonItem slot="header" color="light">
        <IonLabel>Resources</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {backMatter.resources.map((resource: any, index: number) => (
            <IonItem key={resource.uuid || index}>
              <IonLabel>
                <h2>{resource.title}</h2>
                {resource.description && <p>{resource.description}</p>}
                {resource.props && renderProps(resource.props)}
                <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                  {resource.rlinks?.map((rlink: any, rlinkIndex: number) => (
                    <div key={rlinkIndex} style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      {rlink.media_type && (
                        <IonChip color="tertiary" onClick={() => window.open(rlink.href)}>
                          {rlink.media_type.split('/')[1] || rlink.media_type}
                        </IonChip>
                      )}
                    </div>
                  ))}
                </div>
                {resource.citation && (
                  <IonNote className="ion-margin-top">
                    <p><strong>Citation:</strong> {resource.citation.text}</p>
                  </IonNote>
                )}
              </IonLabel>
            </IonItem>
          ))}
        </IonList>
      </div>
    </IonAccordion>
  );
};
const renderParties=(partyUuids: [string, ...string[]]): React.ReactNode => {
  return <>{partyUuids.map(x=><IonBadge>{x}</IonBadge>)}</>
}

const renderComponents = (implementation: any) => {
  if (!implementation?.components?.length) return null;

  return (
    <IonAccordion value="components">
      <IonItem slot="header" color="light">
        <IonLabel>Components</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {implementation.components.map((component: any, index: number) => (
            <IonItem key={component.uuid || index}>
              <IonLabel>
                <div className="by-component-header">
                  <h2>{component.title}</h2>
                  <IonChip color="medium">
                    <IonLabel>{component.type}</IonLabel>
                  </IonChip>
                  {component.status && (
                    <IonChip color="warning">
                      <IonLabel>Status: {component.status.state}</IonLabel>
                    </IonChip>
                  )}
                </div>
                {component.description && <p>{component.description}</p>}
                {component.props && renderProps(component.props)}
                {component['responsible-roles']?.length > 0 && (
                  <div className="roles-container">
                    <h3 className="nested-header">Responsible Roles</h3>
                    {component['responsible-roles'].map((role: ResponsibleRole, roleIndex: number) => (
                      <IonChip key={roleIndex} color="medium">
                        <IonLabel>{role["role-id"]}</IonLabel>
                        {role.props && renderProps(role.props)}
                        {role['party-uuids'] && renderParties(role['party-uuids'])}
                      </IonChip>
                    ))}
                  </div>
                )}
                {component.status?.remarks && (
                  <p className="status-remarks">{component.status.remarks}</p>
                )}
              </IonLabel>
            </IonItem>
          ))}
        </IonList>
      </div>
    </IonAccordion>
  );
};

const renderSystemCharacteristics = (characteristics: any) => {
  if (!characteristics) return null;
  return (
    <IonAccordion value="system-characteristics">
      <IonItem slot="header" color="light">
        <IonLabel>System Characteristics</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {characteristics['system-name'] && (
            <IonItem>
              <IonLabel>
                <h2>System Name</h2>
                <p>{characteristics['system-name']}</p>
              </IonLabel>
            </IonItem>
          )}
          {characteristics.description && (
            <IonItem>
              <IonLabel>
                <h2>Description</h2>
                <p>{characteristics.description}</p>
              </IonLabel>
            </IonItem>
          )}
        </IonList>
      </div>
    </IonAccordion>
  );
};

const renderMetadata = (metadata: any) => {
  if (!metadata) return null;
  return (
    <IonAccordion value="metadata">
      <IonItem slot="header" color="light">
        <IonLabel>Metadata</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {metadata.title && (
            <IonItem>
              <IonLabel>
                <h2>Title</h2>
                <p>{metadata.title}</p>
              </IonLabel>
            </IonItem>
          )}
          {metadata.version && (
            <IonItem>
              <IonLabel>
                <h2>Version</h2>
                <p>{metadata.version}</p>
              </IonLabel>
            </IonItem>
          )}
          {metadata['oscal-version'] && (
            <IonItem>
              <IonLabel>
                <h2>OSCAL Version</h2>
                <p>{metadata['oscal-version']}</p>
              </IonLabel>
            </IonItem>
          )}
        </IonList>
      </div>
    </IonAccordion>
  );
};

const renderControls = (controls: any[], isNested: boolean = false) => {
  if (!controls?.length) return null;
  
  const content = (
    <IonList>
      {controls.map((control, index) => (
            <IonItem key={control.id || index}>
              <IonLabel>
                <h2>{control.title}</h2>
                <p>ID: {control.id}</p>
                {control.props && renderProps(control.props)}
              </IonLabel>
            </IonItem>
      ))}
    </IonList>
  );

  if (isNested) {
    return (
      <div className="nested-content ion-padding">
        <div className="nested-header">
          <h3>Controls</h3>
        </div>
        {content}
      </div>
    );
  }

  return (
    <IonAccordion value="controls">
      <IonItem slot="header" color="light">
        <IonLabel>Controls</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        {content}
      </div>
    </IonAccordion>
  );
};

const renderGroups = (groups: any[]) => {
  if (!groups?.length) return null;
  return (
    <IonAccordion value="groups">
      <IonItem slot="header" color="light">
        <IonLabel>Groups</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {groups.map((group, index) => (
            <div key={group.id || index} className="group-item ion-padding-vertical">
              <h3 className="group-title">{group.title}</h3>
              {group.controls && renderControls(group.controls, true)}
            </div>
          ))}
        </IonList>
      </div>
    </IonAccordion>
  );
};

const ComponentMap: Record<string, React.FC<{content: any}>> = {
  'catalog': ({ content }) => (
    <IonAccordionGroup>
      {renderMetadata(content.metadata)}
      {renderGroups(content.groups)}
      {renderControls(content.controls)}
      {renderBackMatter(content['back-matter'])}
    </IonAccordionGroup>
  ),
  'profile': ({ content }) => (
    <IonAccordionGroup>
      {renderMetadata(content.metadata)}
      {content.imports && (
        <IonAccordion value="imports">
          <IonItem slot="header" color="light">
            <IonLabel>Imports</IonLabel>
          </IonItem>
          <div className="ion-padding" slot="content">
            <IonList>
              {content.imports.map((imp: any, index: number) => (
                <IonItem key={index}>
                  <IonLabel>
                    <p>Href: {imp.href}</p>
                  </IonLabel>
                </IonItem>
              ))}
            </IonList>
          </div>
        </IonAccordion>
      )}
      {renderBackMatter(content['back-matter'])}
    </IonAccordionGroup>
  ),
  'component-definition': ({ content }) => (
    <IonAccordionGroup>
      {renderMetadata(content.metadata)}
      {renderComponents(content)}
      {renderBackMatter(content['back-matter'])}
    </IonAccordionGroup>
  ),
  'system-security-plan': ({ content }) => (
    <IonAccordionGroup>
      {renderMetadata(content.metadata)}
      {renderSystemCharacteristics(content['system-characteristics'])}
      {content['system-implementation'] && renderComponents(content['system-implementation'])}
      {content['control-implementation'] && renderControlImplementation(content['control-implementation'])}
      {renderBackMatter(content['back-matter'])}
    </IonAccordionGroup>
  ),
  'assessment-plan': ({ content }) => (
    <IonAccordionGroup>
      {renderMetadata(content.metadata)}
      {content['import-ssp'] && (
        <IonAccordion value="import-ssp">
          <IonItem slot="header" color="light">
            <IonLabel>Imported SSP</IonLabel>
          </IonItem>
          <div className="ion-padding" slot="content">
            <p>Href: {content['import-ssp'].href}</p>
          </div>
        </IonAccordion>
      )}
      {renderBackMatter(content['back-matter'])}
    </IonAccordionGroup>
  ),
  'assessment-results': ({ content }) => (
    <IonAccordionGroup>
      {renderMetadata(content.metadata)}
      {content.results && content.results.map((result: any, index: number) => (
        <IonAccordion key={index} value={`result-${index}`}>
          <IonItem slot="header" color="light">
            <IonLabel>{result.title || `Result ${index + 1}`}</IonLabel>
          </IonItem>
          <div className="ion-padding" slot="content">
            <p>{result.description}</p>
          </div>
        </IonAccordion>
      ))}
      {renderBackMatter(content['back-matter'])}
    </IonAccordionGroup>
  ),
  'plan-of-action-and-milestones': ({ content }) => (
    <IonAccordionGroup>
      {renderMetadata(content.metadata)}
      {content['poam-items'] && (
        <IonAccordion value="poam-items">
          <IonItem slot="header" color="light">
            <IonLabel>POA&M Items</IonLabel>
          </IonItem>
          <div className="ion-padding" slot="content">
            <IonList>
              {content['poam-items'].map((item: any, index: number) => (
                <IonItem key={index}>
                  <IonLabel>
                    <h2>{item.title}</h2>
                    <p>{item.description}</p>
                  </IonLabel>
                </IonItem>
              ))}
            </IonList>
          </div>
        </IonAccordion>
      )}
      {renderBackMatter(content['back-matter'])}
    </IonAccordionGroup>
  )
};

const RenderOscal: React.FC<RenderOscalProps> = ({ document }) => {
  const type = getDocumentType(document);
  const content = document[type];
  console.log(type,content);
  const Component = ComponentMap[type];
  if (!Component) {
    return (
      <IonAccordionGroup>
        <IonAccordion value="error">
          <IonItem slot="header" color="light">
            <IonLabel>Unsupported Document Type</IonLabel>
          </IonItem>
          <div className="ion-padding" slot="content">
            <p>The document type "{type}" is not currently supported.</p>
          </div>
        </IonAccordion>
      </IonAccordionGroup>
    );
  }

  return <Component content={content} />;
};
function renderControlImplementation(control_implementation: SSPControlImplementation): React.ReactNode {
  return renderImplementedRequirements(control_implementation)
}

export default RenderOscal;

