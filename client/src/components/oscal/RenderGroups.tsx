import React, { useState } from 'react';
import { IonAccordion, IonItem, IonLabel, IonList } from '@ionic/react';
import { ControlGroup } from '../../types';
import { RenderControls } from './RenderControls';
import { RenderProps } from './RenderProps';
import { RenderParts } from './RenderParts';
import { SearchFilterControls, OscalItem } from './SearchFilterControls';

interface RenderGroupsProps {
  groups: ControlGroup[];
}

export const RenderGroups: React.FC<RenderGroupsProps> = ({ groups }) => {
  const [filteredItems, setFilteredItems] = useState<OscalItem[]>([]);
  
  if (!groups?.length) return null;

  const handleFilteredItemsChange = (items: OscalItem[]) => {
    setFilteredItems(items);
  };

  // Get only group items that match the original groups
  const getFilteredGroups = () => {
    if (filteredItems.length === 0) {
      return groups;
    }
    
    return filteredItems
      .filter(item => item.type === 'group')
      .map(item => item.originalGroup)
      .filter((group): group is ControlGroup => group !== undefined);
  };

  return (
    <IonAccordion value="groups">
      <IonItem slot="header" color="light">
        <IonLabel>Groups</IonLabel>
      </IonItem>
      <div className="ion-padding" slot="content">
        <SearchFilterControls 
          catalog={{ groups }} 
          onFilteredItemsChange={handleFilteredItemsChange} 
        />
        
        <div className="groups-container">
          <IonList>
            {getFilteredGroups().map((group, index) => (
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
      </div>
    </IonAccordion>
  );
};
