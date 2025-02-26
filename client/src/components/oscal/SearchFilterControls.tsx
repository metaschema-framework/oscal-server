import React, { useState, useEffect, useMemo } from 'react';
import { 
  IonSearchbar, 
  IonSegment, 
  IonSegmentButton,
  IonLabel,
  IonGrid,
  IonRow,
  IonCol,
  IonBadge
} from '@ionic/react';
import { Control, ControlGroup, Property } from '../../types';

// Define a unified item type that can represent either a control or a group
export interface OscalItem {
  id?: string;
  title?: string;
  type: 'control' | 'group';
  props?: Property[];
  originalControl?: Control;
  originalGroup?: ControlGroup;
  parentGroup?: ControlGroup;
  level: number;
  searchableText?: string; // For indexing
}

interface SearchFilterControlsProps {
  catalog: {
    controls?: Control[];
    groups?: ControlGroup[];
  };
  onFilteredItemsChange?: (items: OscalItem[]) => void;
}

export const SearchFilterControls: React.FC<SearchFilterControlsProps> = ({ 
  catalog,
  onFilteredItemsChange
}) => {
  const [searchText, setSearchText] = useState('');
  const [allItems, setAllItems] = useState<OscalItem[]>([]);
  const [filteredItems, setFilteredItems] = useState<OscalItem[]>([]);
  const [viewMode, setViewMode] = useState<'all' | 'controls' | 'groups'>('all');

  // Create a searchable index from all items
  const createSearchableIndex = (item: OscalItem): string => {
    const parts = [
      item.id || '',
      item.title || '',
      // Include property values in the searchable text
      ...(item.props?.map(p => `${p.name}:${p.value}`) || [])
    ];
    
    return parts.join(' ').toLowerCase();
  };

  // Flatten the controls and groups into a single array of items with searchable index
  useEffect(() => {
    const items: OscalItem[] = [];
    
    // Add top-level controls
    if (catalog.controls) {
      catalog.controls.forEach(control => {
        const item: OscalItem = {
          id: control.id,
          title: control.title,
          type: 'control',
          props: control.props,
          originalControl: control,
          level: 0
        };
        
        // Add searchable text for indexing
        item.searchableText = createSearchableIndex(item);
        items.push(item);
        
        // Add nested controls if any
        if (control.controls) {
          flattenNestedControls(control.controls, items, control, 1);
        }
      });
    }
    
    // Add groups and their controls
    if (catalog.groups) {
      flattenGroups(catalog.groups, items, 0);
    }
    
    setAllItems(items);
    setFilteredItems(items);
  }, [catalog]);

  // Helper function to flatten nested controls
  const flattenNestedControls = (
    controls: Control[], 
    items: OscalItem[], 
    parentControl?: Control, 
    level: number = 0
  ) => {
    controls.forEach(control => {
      const item: OscalItem = {
        id: control.id,
        title: control.title,
        type: 'control',
        props: control.props,
        originalControl: control,
        level
      };
      
      // Add searchable text for indexing
      item.searchableText = createSearchableIndex(item);
      items.push(item);
      
      // Recursively add nested controls if any
      if (control.controls) {
        flattenNestedControls(control.controls, items, control, level + 1);
      }
    });
  };

  // Helper function to flatten groups and their controls
  const flattenGroups = (
    groups: ControlGroup[], 
    items: OscalItem[], 
    level: number = 0,
    parentGroup?: ControlGroup
  ) => {
    groups.forEach(group => {
      // Add the group
      const groupItem: OscalItem = {
        id: group.id,
        title: group.title,
        type: 'group',
        props: group.props,
        originalGroup: group,
        parentGroup,
        level
      };
      
      // Add searchable text for indexing
      groupItem.searchableText = createSearchableIndex(groupItem);
      items.push(groupItem);
      
      // Add controls in this group
      if (group.controls) {
        group.controls.forEach(control => {
          const controlItem: OscalItem = {
            id: control.id,
            title: control.title,
            type: 'control',
            props: control.props,
            originalControl: control,
            parentGroup: group,
            level: level + 1
          };
          
          // Add searchable text for indexing
          controlItem.searchableText = createSearchableIndex(controlItem);
          items.push(controlItem);
          
          // Add nested controls if any
          if (control.controls) {
            flattenNestedControls(control.controls, items, control, level + 2);
          }
        });
      }
      
      // Recursively add nested groups if any
      if (group.groups) {
        flattenGroups(group.groups, items, level + 1, group);
      }
    });
  };

  // Filter items based on search text and view mode
  useEffect(() => {
    const searchLower = searchText.toLowerCase();
    
    const filtered = allItems.filter(item => {
      // Filter by type if needed
      if (viewMode === 'controls' && item.type !== 'control') return false;
      if (viewMode === 'groups' && item.type !== 'group') return false;
      
      // Match by search text using the indexed searchable text
      const matchesSearch = searchText === '' || 
        (item.searchableText && item.searchableText.includes(searchLower));
      
      return matchesSearch;
    });
    
    setFilteredItems(filtered);
    
    if (onFilteredItemsChange) {
      onFilteredItemsChange(filtered);
    }
  }, [searchText, allItems, viewMode, onFilteredItemsChange]);

  // Count controls and groups in filtered items
  const controlCount = filteredItems.filter(item => item.type === 'control').length;
  const groupCount = filteredItems.filter(item => item.type === 'group').length;
  const totalControlCount = allItems.filter(item => item.type === 'control').length;
  const totalGroupCount = allItems.filter(item => item.type === 'group').length;

  return (
    <div className="search-filter-controls">
      <IonSearchbar
        value={searchText}
        onIonChange={e => setSearchText(e.detail.value!)}
        placeholder="Search across all controls and groups..."
        animated={true}
        debounce={300}
      />
      
      <IonGrid>
        <IonRow>
          <IonCol size="12">
            <IonSegment value={viewMode} onIonChange={e => setViewMode(e.detail.value as any)}>
              <IonSegmentButton value="all">
                <IonLabel>All</IonLabel>
              </IonSegmentButton>
              <IonSegmentButton value="controls">
                <IonLabel>Controls</IonLabel>
              </IonSegmentButton>
              <IonSegmentButton value="groups">
                <IonLabel>Groups</IonLabel>
              </IonSegmentButton>
            </IonSegment>
          </IonCol>
        </IonRow>
      </IonGrid>
      
      <div className="search-stats">
        <IonBadge color="primary">{controlCount} controls</IonBadge>
        <IonBadge color="tertiary">{groupCount} groups</IonBadge>
        <small className="total-stats">
          of {totalControlCount} controls and {totalGroupCount} groups
        </small>
      </div>
    </div>
  );
};

export default SearchFilterControls;
