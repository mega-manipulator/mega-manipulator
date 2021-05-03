import React from 'react';
import clsx from 'clsx';
import Layout from '@theme/Layout';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import styles from './index.module.css';

function HomepageHeader() {
  const {siteConfig} = useDocusaurusContext();
  return (
      <header className={clsx('hero hero--primary', styles.heroBanner)}>
        <div className="container">
          <h1 className="hero__title">{siteConfig.title}</h1>
          <p className="hero__subtitle">{siteConfig.tagline}</p>
        </div>
      </header>
  );
}

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
      <Layout
          title={siteConfig.title}
          description="IntelliJ Plugin for doing cross repository search and replace">
        <HomepageHeader/>
        <main>
          <div className="margin--lg">
            <p className="text--center">
              When you know something needs to change, but the problem is everywhere.<br/>
              This plugin for IntelliJ aims to drop most of the overhead, in making changes cross multiple repositories.<br/>
              Even when these are scattered across several code hosts.
            </p>
          </div>
        </main>
      </Layout>
  );
}
