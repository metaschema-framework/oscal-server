import React from 'react';
import { IonAccordion, IonItem, IonLabel, IonList, IonChip } from '@ionic/react';
import { SSPControlImplementation, ResponsibleRole } from '../../types';
import { RenderProps } from './RenderProps';

interface RenderImplementedRequirementsProps {
  implementation: SSPControlImplementation;
}

const renderResponsibleRoles = (roles: ResponsibleRole[]) => {
  if (!roles?.length) return null;

  return (
    <div className="roles-container">
      <h3 className="nested-header">Responsible Roles</h3>
      {roles.map((role, roleIndex) => (
        <IonChip key={roleIndex} color="medium">
          <IonLabel>{role["role-id"]}</IonLabel>
        </IonChip>
      ))}
    </div>
  );
};

export const RenderImplementedRequirements: React.FC<RenderImplementedRequirementsProps> = ({ implementation }) => {
  return (
    <IonAccordion value="implemented-requirements">
      <IonItem slot="header" color="light">
        <IonLabel>Implemented Requirements</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {implementation['implemented-requirements'].map((req, index) => (
            <IonItem key={req.uuid || index}>
              <IonLabel>
                <h2>Control: {req['control-id']}</h2>
                {req.remarks && <p>{req.remarks}</p>}
                {req.props && <RenderProps props={req.props} />}
                {req.statements?.map((statement, stmtIndex) => (
                  <div key={stmtIndex} className="nested-content ion-padding">
                    <h3 className="statement-id">{statement['statement-id']}</h3>
                    {statement.remarks && <p>{statement.remarks}</p>}
                    {statement.props && <RenderProps props={statement.props} />}
                    {statement['by-components']?.map((byComp, compIndex) => (
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
                        {byComp.props && <RenderProps props={byComp.props} />}
                        {byComp['responsible-roles'] && renderResponsibleRoles(byComp['responsible-roles'])}
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
