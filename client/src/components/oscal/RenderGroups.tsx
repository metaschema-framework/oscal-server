import React from 'react';
import { IonAccordion, IonItem, IonLabel, IonList } from '@ionic/react';
import { ControlGroup } from '../../types';
import { RenderControls } from './RenderControls';
import { RenderProps } from './RenderProps';
import { RenderParts } from './RenderParts';

interface RenderGroupsProps {
  groups: ControlGroup[];
}

export const RenderGroups: React.FC<RenderGroupsProps> = ({ groups }) => {
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
              {group.parts && <RenderParts parts={group.parts} />}
              <h3 className="group-title">{group.title}</h3>
              {group.props && <RenderProps props={group.props} />}
              {'controls' in group && <RenderControls 
                      controls={group['controls']||[]} 
                      isNested={true} 
                    />
              }
              {group.groups && group.groups.length > 0 && (
                <div className="nested-content ion-padding">
                  <RenderGroups groups={group.groups} />
                </div>
              )}
            </div>
          ))}
        </IonList>
      </div>
    </IonAccordion>
  );
};
