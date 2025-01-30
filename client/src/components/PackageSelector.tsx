import React, { useEffect, useState } from 'react';
import { IonSelect, IonSelectOption, IonItem, IonLabel, IonSpinner } from '@ionic/react';
import { useOscal } from '../context/OscalContext';

interface PackageSelectorProps {
  label?: string;
}

export const PackageSelector: React.FC<PackageSelectorProps> = ({ label = 'Package' }) => {
  const { packages, packageId, setPackage } = useOscal();
  const [packageList, setPackageList] = useState<string[]>(['workspace']);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadPackages = async () => {
      try {
        const pkgs = await packages();
        setPackageList(pkgs);
      } catch (error) {
        console.error('Failed to load packages:', error);
      } finally {
        setLoading(false);
      }
    };

    loadPackages();
  }, [packages]);

  const handleChange = async (e: CustomEvent) => {
    const newPackageId = e.detail.value;
    try {
      await setPackage(newPackageId);
    } catch (error) {
      console.error('Failed to set package:', error);
    }
  };

  if (loading) {
    return <IonSpinner name="dots" />;
  }

  return (
    <>
      <IonLabel>{label}</IonLabel>
      <IonSelect
        value={packageId}
        onIonChange={handleChange}
        interface="popover"
        placeholder="Select package"
      >
        {packageList.map((pkg) => (
          <IonSelectOption key={pkg} value={pkg}>
            {pkg}
          </IonSelectOption>
        ))}
      </IonSelect>
    </>
  );
};

export default PackageSelector;
