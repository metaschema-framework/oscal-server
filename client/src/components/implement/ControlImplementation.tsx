import React, { useState, useEffect } from 'react';
import {
  IonCard,
  IonCardContent,
  IonCardHeader,
  IonCardTitle,
  IonList,
  IonItem,
  IonLabel,
  IonGrid,
  IonRow,
  IonCol,
  IonButton,
  IonToggle,
  IonText,
  IonChip,
  IonIcon,
  IonBadge,
  IonSegment,
  IonSegmentButton,
  IonProgressBar,
} from '@ionic/react';
import { checkmarkCircle, closeCircle } from 'ionicons/icons';
import { StorageService } from '../../services/storage';
import Search from '../common/Search';

import { Control, Property } from '../../types';
import OscalForm from '../OscalForm';

const ControlImplementation: React.FC = () => {
  const [controls, setControls] = useState<Control[]>([]);
  const [selectedBaseline, setSelectedBaseline] = useState<string>('moderate');
  const [expandedFamilies, setExpandedFamilies] = useState<string[]>([]);
  
  useEffect(() => {
    loadControls();
  }, []);

  const loadControls = async () => {
    try {
      // TODO: Implement actual control loading from StorageService
      const mockControls: Control[] = [
        {
          id: 'AC-1',
          title: 'Access Control Policy and Procedures',
          props: [
            { name: 'baseline', value: 'low' },
            { name: 'baseline', value: 'moderate' },
            { name: 'baseline', value: 'high' },
            { name: 'implemented', value: 'true' }
          ],
          parts: [{
            name: 'description',
            prose: 'The organization develops, documents, and disseminates access control policies and procedures.'
          }]
        },
        {
          id: 'AC-2',
          title: 'Account Management',
          props: [
            { name: 'baseline', value: 'low' },
            { name: 'baseline', value: 'moderate' },
            { name: 'baseline', value: 'high' },
            { name: 'implemented', value: 'false' }
          ],
          parts: [{
            name: 'description',
            prose: 'The organization manages information system accounts, including establishing, activating, modifying, reviewing, disabling, and removing accounts.'
          }]
        },
        {
          id: 'AU-1',
          title: 'Audit and Accountability Policy and Procedures',
          props: [
            { name: 'baseline', value: 'low' },
            { name: 'baseline', value: 'moderate' },
            { name: 'baseline', value: 'high' },
            { name: 'implemented', value: 'true' }
          ],
          parts: [{
            name: 'description',
            prose: 'The organization develops, documents, and disseminates audit and accountability policies and procedures.'
          }]
        },
        {
          id: 'AU-2',
          title: 'Audit Events',
          props: [
            { name: 'baseline', value: 'low' },
            { name: 'baseline', value: 'moderate' },
            { name: 'baseline', value: 'high' },
            { name: 'implemented', value: 'false' }
          ],
          parts: [{
            name: 'description',
            prose: 'The organization determines that the information system is capable of auditing specific events.'
          }]
        },
        {
          id: 'CM-1',
          title: 'Configuration Management Policy and Procedures',
          props: [
            { name: 'baseline', value: 'moderate' },
            { name: 'baseline', value: 'high' },
            { name: 'implemented', value: 'true' }
          ],
          parts: [{
            name: 'description',
            prose: 'The organization develops, documents, and disseminates configuration management policies and procedures.'
          }]
        }
      ];
      setControls(mockControls);
    } catch (error) {
      console.error('Error loading controls:', error);
    }
  };

  const handleBaselineChange = (event: CustomEvent) => {
    setSelectedBaseline(event.detail.value);
  };

  const toggleFamily = (family: string) => {
    setExpandedFamilies(prev => 
      prev.includes(family) 
        ? prev.filter(f => f !== family)
        : [...prev, family]
    );
  };

  const getControlFamily = (controlId: string) => {
    return controlId.split('-')[0];
  };

  const groupControlsByFamily = (controls: Control[]) => {
    return controls.reduce((acc, control) => {
      const family = getControlFamily(control.id);
      if (!acc[family]) {
        acc[family] = [];
      }
      acc[family].push(control);
      return acc;
    }, {} as Record<string, Control[]>);
  };

  const calculateFamilyProgress = (controls: Control[]) => {
    const implemented = controls.filter(control => 
      control.props?.find(p => p.name === 'implemented')?.value === 'true'
    ).length;
    return implemented / controls.length;
  };

  const handleImplementationToggle = (controlId: string) => {
    setControls(prevControls => {
      return prevControls.map(control => {
        if (control.id === controlId) {
          const props = [...(control.props || [{ name: 'implemented', value: 'false' }])];
          const implIndex = props.findIndex(p => p.name === 'implemented');
          const newValue = props[implIndex]?.value === 'true' ? 'false' : 'true';
          props[implIndex] = { ...props[implIndex], value: newValue };
          
          const updatedControl: Control = { 
            ...control, 
            props: props as [Property, ...Property[]]
          };
          return updatedControl;
        }
        return control;
      });
    });
  };

  const filteredControls = selectedBaseline
    ? controls.filter(control => 
        control.props?.some(p => 
          p.name === 'baseline' && p.value === selectedBaseline
        )
      )
    : controls;

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Control Implementation</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <IonGrid>
          <IonRow>
            <IonCol>
              <IonSegment value={selectedBaseline} onIonChange={handleBaselineChange}>
                <IonSegmentButton value="low">
                  <IonLabel>Low</IonLabel>
                </IonSegmentButton>
                <IonSegmentButton value="moderate">
                  <IonLabel>Moderate</IonLabel>
                </IonSegmentButton>
                <IonSegmentButton value="high">
                  <IonLabel>High</IonLabel>
                </IonSegmentButton>
              </IonSegment>
            </IonCol>
          </IonRow>
          <IonRow>
            <IonCol>
              <Search context="controls" />
            </IonCol>
          </IonRow>
          <IonRow>
            <IonCol>
              <OscalForm type='control-implementation' onSubmit={()=>{}} />
            </IonCol>
          </IonRow>
          <IonRow>
            <IonCol>
              {Object.entries(groupControlsByFamily(filteredControls)).map(([family, familyControls]) => (
                <IonCard key={family} style={{ marginBottom: '1rem' }}>
                  <IonItem 
                    button 
                    onClick={() => toggleFamily(family)}
                    style={{ cursor: 'pointer' }}
                  >
                    <IonLabel>
                      <h2 style={{ fontWeight: 'bold' }}>
                        {family} Controls
                        <IonBadge 
                          color="medium" 
                          style={{ marginLeft: '0.5rem' }}
                        >
                          {familyControls.length}
                        </IonBadge>
                      </h2>
                    </IonLabel>
                    <IonProgressBar 
                      value={calculateFamilyProgress(familyControls)}
                      color="primary"
                      style={{ height: '0.5rem', borderRadius: '4px' }}
                    />
                  </IonItem>
                  
                  {expandedFamilies.includes(family) && (
                    <IonList>
                      {familyControls.map(control => (
                        <IonItem key={control.id} style={{ '--padding-start': '1rem' }}>
                          <IonLabel>
                            <IonGrid>
                              <IonRow>
                                <IonCol size="12">
                                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                    <h2 style={{ margin: 0 }}>{control.id}: {control.title}</h2>
                                    <IonIcon
                                      icon={control.props?.find(p => p.name === 'implemented')?.value === 'true' 
                                        ? checkmarkCircle 
                                        : closeCircle}
                                      color={control.props?.find(p => p.name === 'implemented')?.value === 'true'
                                        ? 'success'
                                        : 'medium'}
                                    />
                                  </div>
                                </IonCol>
                              </IonRow>
                              <IonRow>
                                <IonCol>
                                  <IonText color="medium">
                                    <p style={{ margin: '0.5rem 0' }}>{control.parts?.[0]?.prose}</p>
                                  </IonText>
                                </IonCol>
                              </IonRow>
                              <IonRow>
                                <IonCol>
                                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                                    <IonChip color="primary">
                                      {selectedBaseline}
                                    </IonChip>
                                    <IonToggle
                                      checked={control.props?.find(p => p.name === 'implemented')?.value === 'true'}
                                      onIonChange={() => handleImplementationToggle(control.id)}
                                    />
                                  </div>
                                </IonCol>
                              </IonRow>
                            </IonGrid>
                          </IonLabel>
                        </IonItem>
                      ))}
                    </IonList>
                  )}
                </IonCard>
              ))}
            </IonCol>
          </IonRow>
        </IonGrid>
      </IonCardContent>
    </IonCard>
  );
};

export default ControlImplementation;
