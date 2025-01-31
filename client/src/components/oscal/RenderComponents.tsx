import React from 'react';
import { IonAccordion, IonItem, IonLabel, IonList, IonChip } from '@ionic/react';
import { Component, ResponsibleRole } from '../../types';
import { RenderProps } from './RenderProps';

interface RenderComponentsProps {
  components: Component[];
}

const renderParties = (partyUuids: string[]) => {
  return partyUuids.map(uuid => (
    <IonChip key={uuid} color="light">
      <IonLabel>{uuid}</IonLabel>
    </IonChip>
  ));
};

const renderResponsibleRoles = (roles: ResponsibleRole[]) => {
  if (!roles?.length) return null;

  return (
    <div className="roles-container">
      <h3 className="nested-header">Responsible Roles</h3>
      {roles.map((role, roleIndex) => (
        <div key={roleIndex}>
          <IonChip color="medium">
            <IonLabel>{role["role-id"]}</IonLabel>
          </IonChip>
          {role.props && <RenderProps props={role.props} />}
          {role["party-uuids"] && renderParties(role["party-uuids"])}
        </div>
      ))}
    </div>
  );
};

export const RenderComponents: React.FC<RenderComponentsProps> = ({ components }) => {
  if (!components?.length) return null;

  return (
    <IonAccordion value="components">
      <IonItem slot="header" color="light">
        <IonLabel>Components</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <IonList>
          {components.map((component, index) => (
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
                {component.purpose && <p><strong>Purpose:</strong> {component.purpose}</p>}
                {component.props && <RenderProps props={component.props} />}
                {component["responsible-roles"] && renderResponsibleRoles(component["responsible-roles"])}
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
